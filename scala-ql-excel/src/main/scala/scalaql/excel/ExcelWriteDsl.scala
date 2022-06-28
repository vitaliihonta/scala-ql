package scalaql.excel

import scalaql.excel.internal.ExcelDataSourceWriter
import scalaql.sources.*
import java.io.OutputStream

class ExcelWriteDsl[A](override protected val _config: ExcelWriteConfig[A])
    extends DataSourceWriteDsl[A, OutputStream, ExcelEncoder, ExcelWriteConfig, ExcelDataSourceWriter, ExcelWriteDsl[A]]
    with DataSourceJavaOutputStreamWriteDslMixin[
      A,
      ExcelEncoder,
      ExcelWriteConfig,
      ExcelDataSourceWriter,
      ExcelWriteDsl[A]
    ]
    with DataSourceFilesWriteDslMixin[
      A,
      OutputStream,
      ExcelEncoder,
      ExcelWriteConfig,
      ExcelDataSourceWriter,
      ExcelWriteDsl[
        A
      ]
    ] {

  override protected val _writer = ExcelDataSourceWriter

  override def config(config: ExcelWriteConfig[A]): ExcelWriteDsl[A] =
    new ExcelWriteDsl[A](config)

  def option(naming: Naming): ExcelWriteDsl[A] =
    config(_config.copy(naming = naming))

  def option(headers: Boolean): ExcelWriteDsl[A] =
    config(_config.copy(writeHeaders = headers))

  def option(worksheetName: String): ExcelWriteDsl[A] =
    config(_config.copy(worksheetName = Some(worksheetName)))

  def option(styling: ExcelStyling[A]): ExcelWriteDsl[A] =
    config(_config.copy(styling = styling))
}
