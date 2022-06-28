package scalaql.excel.internal

import org.apache.poi.ss.usermodel.{Cell, CellType, Row}
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import scalaql.excel.{CellResolutionStrategy, ExcelDecoder, ExcelReadConfig, ExcelReadContext}
import scalaql.sources.{DataSourceJavaInputStreamReader, DataSourceJavaInputStreamReaderFilesSupport, Naming}
import scalaql.sources.columnar.CodecPath
import scala.jdk.CollectionConverters.*
import java.io.InputStream

object ExcelDataSourceReader extends ExcelDataSourceReader

class ExcelDataSourceReader
    extends DataSourceJavaInputStreamReader[ExcelDecoder, λ[a => ExcelReadConfig]]
    with DataSourceJavaInputStreamReaderFilesSupport[ExcelDecoder, λ[a => ExcelReadConfig]] {

  protected def readImpl[A: ExcelDecoder](inputStream: InputStream)(implicit config: ExcelReadConfig): Iterable[A] = {
    val workbook    = new XSSFWorkbook(inputStream)
    val worksheet   = config.choseWorksheet(workbook)
    val rowIterator = worksheet.iterator().asScala

    implicit val ctx: ExcelReadContext = initialContext(workbook, rowIterator, config.naming)

    rowIterator.zipWithIndex
      .filterNot { case (row, _) => isBlankRow(row) }
      .map { case (row, idx) =>
        ExcelDecoder[A]
          .read(row)(ctx.copy(documentRowNumber = idx))
          .fold[A](throw _, _.value)
      }
      .toVector
  }

  private def initialContext(
    workbook:        XSSFWorkbook,
    rowIterator:     Iterator[Row],
    naming:          Naming
  )(implicit config: ExcelReadConfig
  ): ExcelReadContext = {
    val headers = inferHeaders(rowIterator)
    ExcelReadContext(
      workbook = workbook,
      naming = naming,
      evaluateFormulas = config.evaluateFormulas,
      headers = headers,
      config.cellResolutionStrategy,
      location = CodecPath.Root,
      currentOffset = 0,
      documentRowNumber = 0
    )
  }

  // Usually Excel documents can have rows with blanks cells.
  // We're not interested in reading them
  private def isBlankRow(row: Row): Boolean =
    row.cellIterator().asScala.forall(isBlankCell)

  private def isBlankCell(cell: Cell): Boolean =
    cell.getCellType == CellType.BLANK

  private def inferHeaders(rowIterator: Iterator[Row])(implicit config: ExcelReadConfig): Map[String, Int] =
    config.cellResolutionStrategy match {
      case CellResolutionStrategy.NameBased =>
        readHeadersFromRow(rowIterator.next())
      case _ => Map.empty[String, Int]
    }

  private def readHeadersFromRow(headersRow: Row): Map[String, Int] =
    headersRow
      .iterator()
      .asScala
      .zipWithIndex
      .filterNot { case (cell, _) => isBlankCell(cell) }
      .map { case (cell, idx) =>
        if (cell.getCellType == CellType.STRING) cell.getStringCellValue -> idx
        else
          throw new IllegalArgumentException(
            s"Name based cell resolution strategy chosen, but first row cells are not strings" +
              s" (especially cell $idx of type ${cell.getCellType})"
          )
      }
      .toMap
}
