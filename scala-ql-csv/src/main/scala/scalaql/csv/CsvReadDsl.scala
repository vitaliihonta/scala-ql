package scalaql.csv

import scalaql.Naming
import scalaql.csv.internal.CsvDataSourceReader
import scalaql.sources.{DataSourceFilesReadDslMixin, DataSourceJavaIOReadDslMixin, DataSourceReadDsl}
import java.io.Reader

class CsvReadDsl[A](override val config: CsvReadConfig)
    extends DataSourceReadDsl[A, Reader, CsvDecoder, λ[a => CsvReadConfig], CsvDataSourceReader, CsvReadDsl[A]]
    with DataSourceJavaIOReadDslMixin[A, CsvDecoder, λ[a => CsvReadConfig], CsvDataSourceReader, CsvReadDsl[A]]
    with DataSourceFilesReadDslMixin[A, Reader, CsvDecoder, λ[a => CsvReadConfig], CsvDataSourceReader, CsvReadDsl[A]] {

  override protected val _reader = CsvDataSourceReader

  override def withConfig(config: CsvReadConfig): CsvReadDsl[A] =
    new CsvReadDsl[A](config)

  def option(naming: Naming): CsvReadDsl[A] =
    withConfig(config.copy(naming = naming))

  def option(caseSensitive: Boolean): CsvReadDsl[A] =
    withConfig(config.copy(caseSensitive = caseSensitive))

  def options(
    delimiter:           Char = config.delimiter,
    quoteChar:           Char = config.quoteChar,
    escapeChar:          Char = config.escapeChar,
    lineTerminator:      String = config.lineTerminator,
    quoting:             Quoting = config.quoting,
    treatEmptyLineAsNil: Boolean = config.treatEmptyLineAsNil,
    naming:              Naming = config.naming,
    caseSensitive:       Boolean = config.caseSensitive
  ): CsvReadDsl[A] =
    withConfig(
      config.copy(
        delimiter = delimiter,
        quoteChar = quoteChar,
        escapeChar = escapeChar,
        lineTerminator = lineTerminator,
        quoting = quoting,
        treatEmptyLineAsNil = treatEmptyLineAsNil,
        naming = naming,
        caseSensitive = caseSensitive
      )
    )
}
