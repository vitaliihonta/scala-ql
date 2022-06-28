package scalaql.csv

import scalaql.csv.internal.CsvDataSourceReader
import scalaql.sources.{DataSourceFilesReadDslMixin, DataSourceJavaIOReadDslMixin, DataSourceReadDsl, Naming}
import java.io.Reader

class CsvReadDsl[A](override protected val _config: CsvReadConfig)
    extends DataSourceReadDsl[A, Reader, CsvDecoder, λ[a => CsvReadConfig], CsvDataSourceReader, CsvReadDsl[A]]
    with DataSourceJavaIOReadDslMixin[A, CsvDecoder, λ[a => CsvReadConfig], CsvDataSourceReader, CsvReadDsl[A]]
    with DataSourceFilesReadDslMixin[A, Reader, CsvDecoder, λ[a => CsvReadConfig], CsvDataSourceReader, CsvReadDsl[A]] {

  override protected val _reader = CsvDataSourceReader

  override def config(config: CsvReadConfig): CsvReadDsl[A] =
    new CsvReadDsl[A](config)

  def option(naming: Naming): CsvReadDsl[A] =
    config(_config.copy(naming = naming))

  def option(caseSensitive: Boolean): CsvReadDsl[A] =
    config(_config.copy(caseSensitive = caseSensitive))
}
