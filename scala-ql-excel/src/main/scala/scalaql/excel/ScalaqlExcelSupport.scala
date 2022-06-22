package scalaql.excel

import scalaql.sources.*
import org.apache.poi.ss.usermodel.{Cell, Row}
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import scalaql.SideEffect
import scalaql.excel.ExcelTableApi.ConfigureCell
import scalaql.sources.columnar.CodecPath
import scalaql.sources.columnar.TableApiFunctions

import java.nio.charset.StandardCharsets
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import scala.jdk.CollectionConverters.*

trait ScalaqlExcelSupport
    extends DataSourceJavaStreamsSupport[ExcelDecoder, ExcelEncoder, λ[a => ExcelReadConfig], ExcelWriteConfig] {
  final object read
      extends DataSourceJavaInputStreamReader[ExcelDecoder, λ[a => ExcelReadConfig]]
      with DataSourceJavaInputStreamReaderFilesSupport[ExcelDecoder, λ[a => ExcelReadConfig]] {

    protected def readImpl[A: ExcelDecoder](inputStream: InputStream)(implicit config: ExcelReadConfig): Iterable[A] = {
      val workbook    = new XSSFWorkbook(inputStream)
      val worksheet   = config.choseWorksheet(workbook)
      val rowIterator = worksheet.iterator().asScala

      implicit val ctx: ExcelReadContext = initialContext(workbook, rowIterator, config.naming)

      rowIterator.map(ExcelDecoder[A].read(_).fold[A](throw _, _.value)).toVector
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
        currentOffset = 0
      )
    }
  }

  final object write
      extends DataSourceJavaOutputStreamWriter[ExcelEncoder, ExcelWriteConfig]
      with DataSourceJavaOutputStreamWriteFilesSupport[ExcelEncoder, ExcelWriteConfig] {

    def write[A: ExcelEncoder](
      sink:            => OutputStream
    )(implicit config: ExcelWriteConfig[A]
    ): SideEffect[?, ?, A] = {
      val workbook  = new XSSFWorkbook()
      val worksheet = workbook.createSheet(config.worksheetName.getOrElse("Default"))
      val table     = new ExcelTableApi(worksheet, ExcelEncoder[A].headers)
      SideEffect[OutputStream, Int, A](
        initialState = 0,
        acquire = () => sink,
        release = { (os, _) =>
          implicit val writeContext: ExcelWriteContext = ExcelWriteContext.initial(
            workbook = workbook,
            headers = ExcelEncoder[A].headers,
            cellStyle = config.styling.cellStyle
          )
          TableApiFunctions.fillGapsIntoTable[Cell, ConfigureCell, ExcelTableRowApi](table)(header =>
            (cell: Cell) => {
              config.styling.cellStyle(header).foreach { styling =>
                val cellStyle = workbook.createCellStyle()
                styling(workbook, cellStyle)
                cell.setCellStyle(cellStyle)
              }
              cell.setBlank()
              cell
            }
          )
          workbook.write(os)
          os.flush()
          os.close()
        },
        use = { (_, rowIdx, value) => writeRow[A](workbook, table, rowIdx, value) }
      )
    }

    private def writeRow[A: ExcelEncoder](
      workbook:        XSSFWorkbook,
      table:           ExcelTableApi,
      rowIdx:          Int,
      value:           A
    )(implicit config: ExcelWriteConfig[A]
    ): Int = {
      val idx = if (rowIdx == 0 && config.writeHeaders) {
        writeHeaders[A](workbook, table)
        rowIdx + 1
      } else rowIdx

      ExcelEncoder[A].write(value, table.appendEmptyRow)(
        ExcelWriteContext.initial(
          workbook = workbook,
          headers = ExcelEncoder[A].headers,
          cellStyle = config.styling.cellStyle
        )
      )
      idx + 1
    }
  }

  private def writeHeaders[A: ExcelEncoder](
    workbook:        XSSFWorkbook,
    table:           ExcelTableApi
  )(implicit config: ExcelWriteConfig[A]
  ): Unit = {
    table.appendEmptyRow
    val headerRow = table.currentRow
    for ((header, idx) <- ExcelEncoder[A].headers.zipWithIndex)
      headerRow.insert(
        idx,
        header,
        (cell: Cell) => {
          config.styling.headerStyle(header).foreach { styling =>
            val cellStyle = workbook.createCellStyle()
            styling(workbook, cellStyle)
            cell.setCellStyle(cellStyle)
          }
          cell.setCellValue(config.naming(header))
          cell
        }
      )
  }

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
