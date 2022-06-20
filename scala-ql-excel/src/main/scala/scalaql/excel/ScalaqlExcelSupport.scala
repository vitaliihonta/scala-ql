package scalaql.excel

import scalaql.sources.*
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import scalaql.SideEffect
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

      implicit val ctx: ExcelReadContext = initialContext(workbook, rowIterator)

      rowIterator.map(ExcelDecoder[A].read(_).value).toVector
    }

    private def initialContext(
      workbook:        XSSFWorkbook,
      rowIterator:     Iterator[Row]
    )(implicit config: ExcelReadConfig
    ): ExcelReadContext = {
      val headers = inferHeaders(rowIterator)
      ExcelReadContext(
        workbook = workbook,
        evaluateFormulas = config.evaluateFormulas,
        headers = headers,
        config.cellResolutionStrategy,
        path = Nil,
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
      SideEffect[OutputStream, Int, A](
        initialState = 0,
        acquire = () => sink,
        release = { (os, _) =>
          workbook.write(os)
          os.flush()
          os.close()
        },
        use = { (_, rowIdx, value) => writeRow[A](workbook, worksheet, rowIdx, value) }
      )
    }

    private def writeRow[A: ExcelEncoder](
      workbook:        XSSFWorkbook,
      worksheet:       XSSFSheet,
      rowIdx:          Int,
      value:           A
    )(implicit config: ExcelWriteConfig[A]
    ): Int = {
      val idx = if (rowIdx == 0 && config.writeHeaders) {
        writeHeaders[A](workbook, worksheet)
        rowIdx + 1
      } else rowIdx

      val row = worksheet.createRow(idx)

      ExcelEncoder[A].write(row, value)(
        ExcelWriteContext(
          workbook = workbook,
          path = Nil,
          startOffset = 0,
          cellStyle = config.styling.cellStyle
        )
      )
      idx + 1
    }
  }

  private def writeHeaders[A: ExcelEncoder](
    workbook:        XSSFWorkbook,
    worksheet:       XSSFSheet
  )(implicit config: ExcelWriteConfig[A]
  ): Unit = {
    val headerRow = worksheet.createRow(0)
    for ((header, idx) <- ExcelEncoder[A].headers.zipWithIndex) {
      val cell = headerRow.createCell(idx)
      config.styling.headerStyle(header).foreach { styling =>
        val cellStyle = workbook.createCellStyle()
        styling(workbook, cellStyle)
        cell.setCellStyle(cellStyle)
      }
      cell.setCellValue(config.naming(header))
    }
  }

  private def inferHeaders(rowIterator: Iterator[Row])(implicit config: ExcelReadConfig): Map[String, Int] =
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
