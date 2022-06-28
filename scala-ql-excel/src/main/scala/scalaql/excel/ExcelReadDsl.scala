package scalaql.excel

import org.apache.poi.ss.usermodel.{Sheet, Workbook}
import scalaql.excel.internal.ExcelDataSourceReader
import scalaql.sources.*
import java.io.InputStream

class ExcelReadDsl[A](override protected val _config: ExcelReadConfig)
    extends DataSourceReadDsl[
      A,
      InputStream,
      ExcelDecoder,
      λ[a => ExcelReadConfig],
      ExcelDataSourceReader,
      ExcelReadDsl[A]
    ]
    with DataSourceJavaInputStreamReadDslMixin[
      A,
      ExcelDecoder,
      λ[a => ExcelReadConfig],
      ExcelDataSourceReader,
      ExcelReadDsl[A]
    ]
    with DataSourceFilesReadDslMixin[
      A,
      InputStream,
      ExcelDecoder,
      λ[a => ExcelReadConfig],
      ExcelDataSourceReader,
      ExcelReadDsl[A]
    ] {

  override protected val _reader = ExcelDataSourceReader

  override def config(config: ExcelReadConfig): ExcelReadDsl[A] =
    new ExcelReadDsl[A](config)

  def option(choseWorksheet: Workbook => Sheet): ExcelReadDsl[A] =
    config(_config.copy(choseWorksheet = choseWorksheet))

  def option(naming: Naming): ExcelReadDsl[A] =
    config(_config.copy(naming = naming))

  def option(evaluateFormulas: Boolean): ExcelReadDsl[A] =
    config(_config.copy(evaluateFormulas = evaluateFormulas))

  def option(cellResolutionStrategy: CellResolutionStrategy): ExcelReadDsl[A] =
    config(_config.copy(cellResolutionStrategy = cellResolutionStrategy))
}
