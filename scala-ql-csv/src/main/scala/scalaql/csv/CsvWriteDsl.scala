package scalaql.csv

import scalaql.csv.internal.CsvDataSourceWriter
import scalaql.sources.{DataSourceFilesWriteDslMixin, DataSourceJavaIOWriteDslMixin, DataSourceWriteDsl, Naming}
import java.io.Writer

class CsvWriteDsl[A](override val config: CsvWriteConfig)
    extends DataSourceWriteDsl[A, Writer, CsvEncoder, λ[a => CsvWriteConfig], CsvDataSourceWriter, CsvWriteDsl[A]]
    with DataSourceJavaIOWriteDslMixin[A, CsvEncoder, λ[a => CsvWriteConfig], CsvDataSourceWriter, CsvWriteDsl[A]]
    with DataSourceFilesWriteDslMixin[A, Writer, CsvEncoder, λ[a => CsvWriteConfig], CsvDataSourceWriter, CsvWriteDsl[
      A
    ]] {

  override protected val _writer = CsvDataSourceWriter

  override def withConfig(config: CsvWriteConfig): CsvWriteDsl[A] =
    new CsvWriteDsl[A](config)

  def option(naming: Naming): CsvWriteDsl[A] =
    withConfig(config.copy(naming = naming))

  def options(
    delimiter:           Char = config.delimiter,
    quoteChar:           Char = config.quoteChar,
    escapeChar:          Char = config.escapeChar,
    lineTerminator:      String = config.lineTerminator,
    quoting:             Quoting = config.quoting,
    treatEmptyLineAsNil: Boolean = config.treatEmptyLineAsNil,
    naming:              Naming = config.naming
  ): CsvWriteDsl[A] =
    withConfig(
      config.copy(
        delimiter = delimiter,
        quoteChar = quoteChar,
        escapeChar = escapeChar,
        lineTerminator = lineTerminator,
        quoting = quoting,
        treatEmptyLineAsNil = treatEmptyLineAsNil,
        naming = naming
      )
    )
}
