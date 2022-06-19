package scalaql.excel

import scalaql.sources.*
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.nio.charset.StandardCharsets
import java.io.InputStream
import java.io.Reader
import scala.jdk.CollectionConverters.*

trait ScalaqlExcelSupport extends DataSourceJavaInputStreamReadSupport[ExcelDecoder, ExcelConfig] {
  final object read
      extends DataSourceJavaInputStreamReader[ExcelDecoder, ExcelConfig]
      with DataSourceJavaInputStreamReaderFilesSupport[ExcelDecoder, ExcelConfig] {

    protected def readImpl[A: ExcelDecoder](inputStream: InputStream)(implicit config: ExcelConfig): Iterable[A] = {
      val workbook    = new XSSFWorkbook(inputStream)
      val worksheet   = config.choseWorksheet(workbook)
      val rowIterator = worksheet.iterator().asScala
      val headers     = inferHeaders(rowIterator)
      implicit val ctx: ExcelContext = ExcelContext(
        workbook,
        config.evaluateFormulas,
        headers,
        config.cellResolutionStrategy,
        path = Nil,
        currentOffset = 0
      )
      rowIterator.map(ExcelDecoder[A].read(_).value).toVector
    }
  }

  private def inferHeaders(rowIterator: Iterator[Row])(implicit config: ExcelConfig): Map[String, Int] =
    config.cellResolutionStrategy match {
      case _: CellResolutionStrategy.NameBased =>
        readHeadersFromRow(rowIterator.next())
      case _ => Map.empty[String, Int]
    }

  private def readHeadersFromRow(headersRow: Row): Map[String, Int] =
    headersRow
      .iterator()
      .asScala
      .zipWithIndex
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
