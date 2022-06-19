package scalaql.excel

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.commons.io
import org.apache.commons.io.input.ReaderInputStream
import scalaql.sources.*
import java.nio.charset.StandardCharsets
import java.io.Reader
import scala.jdk.CollectionConverters.*

trait ScalaqlExcelSupport extends DataSourceReadSupport[ExcelDecoder.Row, ExcelConfig] {
  final object read extends DataSourceReader[ExcelDecoder.Row, ExcelConfig] {
    protected def readImpl[A: ExcelDecoder.Row](reader: Reader)(implicit config: ExcelConfig): Iterable[A] = {
      val workbook                    = new HSSFWorkbook(new ReaderInputStream(reader, StandardCharsets.UTF_8))
      val worksheet                   = config.choseWorksheet(workbook)
      implicit val ctx: ReaderContext = ReaderContext(workbook, evaluateFormulas = config.evaluateFormulas)
      worksheet
        .iterator()
        .asScala
        .map(ExcelDecoder.Row[A].readRow(_))
        .toVector
    }
  }
}
