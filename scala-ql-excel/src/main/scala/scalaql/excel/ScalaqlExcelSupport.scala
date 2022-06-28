package scalaql.excel

import scalaql.sources.*
import org.apache.poi.ss.usermodel.{Cell, Row}
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import scalaql.SideEffect
import scalaql.excel.ExcelTableApi.ConfigureCell
import scalaql.excel.internal.{ExcelDataSourceReader, ExcelDataSourceWriter}
import scalaql.sources.columnar.CodecPath
import scalaql.sources.columnar.TableApiFunctions
import java.nio.charset.StandardCharsets
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import scala.jdk.CollectionConverters.*

trait ScalaqlExcelSupport
    extends DataSourceJavaStreamsSupport[
      ExcelDecoder,
      ExcelEncoder,
      λ[a => ExcelReadConfig],
      ExcelWriteConfig,
      ExcelDataSourceReader,
      ExcelDataSourceWriter,
      ExcelReadDsl,
      ExcelWriteDsl
    ] {

  override def read[A]: ExcelReadDsl[A]   = new ExcelReadDsl[A](ExcelReadConfig.default)
  override def write[A]: ExcelWriteDsl[A] = new ExcelWriteDsl[A](ExcelWriteConfig.default)
}
