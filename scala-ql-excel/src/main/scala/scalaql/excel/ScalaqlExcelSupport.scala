package scalaql.excel

import scalaql.sources.*
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import scalaql.SideEffect
import java.nio.charset.StandardCharsets
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import scala.jdk.CollectionConverters.*

trait ScalaqlExcelSupport
    extends DataSourceJavaStreamsSupport[ExcelDecoder, ExcelEncoder, ExcelReadConfig, ExcelWriteConfig] {
  final object read
      extends DataSourceJavaInputStreamReader[ExcelDecoder, ExcelReadConfig]
      with DataSourceJavaInputStreamReaderFilesSupport[ExcelDecoder, ExcelReadConfig] {

    protected def readImpl[A: ExcelDecoder](inputStream: InputStream)(implicit config: ExcelReadConfig): Iterable[A] = {
      val workbook    = new XSSFWorkbook(inputStream)
      val worksheet   = config.choseWorksheet(workbook)
      val rowIterator = worksheet.iterator().asScala
      val headers     = inferHeaders(rowIterator)
      implicit val ctx: ExcelReadContext = ExcelReadContext(
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

  final object write
      extends DataSourceJavaOutputStreamWriter[ExcelEncoder, ExcelWriteConfig]
      with DataSourceJavaOutputStreamWriteFilesSupport[ExcelEncoder, ExcelWriteConfig] {

    def write[A: ExcelEncoder](
      sink:            => OutputStream
    )(implicit config: ExcelWriteConfig
    ): SideEffect[?, ?, A] = {
      val headers   = ExcelEncoder[A].headers
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
        use = { (_, rowIdx, value) =>
          val idx = if (rowIdx == 0 && config.writeHeaders) {
            val headerRow = worksheet.createRow(0)
            for ((header, idx) <- headers.zipWithIndex)
              headerRow.createCell(idx).setCellValue(config.naming(header))
            rowIdx + 1
          } else rowIdx

          val row = worksheet.createRow(idx)
          ExcelEncoder[A].write(row, value)(
            ExcelWriteContext(
              workbook = workbook,
              path = Nil,
              startOffset = 0
            )
          )
          idx + 1
        }
      )
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
