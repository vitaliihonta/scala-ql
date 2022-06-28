package scalaql.excel

import scalaql.excel.internal.ExcelDataSourceWriter
import scalaql.sources.*
import java.io.OutputStream

class ExcelWriteDsl[A](override val config: ExcelWriteConfig[A])
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

  override def withConfig(config: ExcelWriteConfig[A]): ExcelWriteDsl[A] =
    new ExcelWriteDsl[A](config)

  def option(naming: Naming): ExcelWriteDsl[A] =
    withConfig(config.copy(naming = naming))

  def option(headers: Boolean): ExcelWriteDsl[A] =
    withConfig(config.copy(headers = headers))

  def option(worksheetName: String): ExcelWriteDsl[A] =
    withConfig(config.copy(worksheetName = Some(worksheetName)))

  def option(styling: ExcelStyling[A]): ExcelWriteDsl[A] =
    withConfig(config.copy(styling = styling))

  def options(
    worksheetName: Option[String] = config.worksheetName,
    headers:       Boolean = config.headers,
    naming:        Naming = config.naming,
    styling:       ExcelStyling[A] = config.styling
  ): ExcelWriteDsl[A] =
    withConfig(
      config.copy(
        worksheetName = worksheetName,
        headers = headers,
        naming = naming,
        styling = styling
      )
    )
}
