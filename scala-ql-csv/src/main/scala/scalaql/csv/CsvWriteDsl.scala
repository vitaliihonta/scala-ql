package scalaql.csv

import scalaql.csv.internal.CsvDataSourceWriter
import scalaql.sources.{DataSourceFilesWriteDslMixin, DataSourceJavaIOWriteDslMixin, DataSourceWriteDsl, Naming}
import java.io.Writer

class CsvWriteDsl[A](override protected val _config: CsvWriteConfig)
    extends DataSourceWriteDsl[A, Writer, CsvEncoder, λ[a => CsvWriteConfig], CsvDataSourceWriter, CsvWriteDsl[A]]
    with DataSourceJavaIOWriteDslMixin[A, CsvEncoder, λ[a => CsvWriteConfig], CsvDataSourceWriter, CsvWriteDsl[A]]
    with DataSourceFilesWriteDslMixin[A, Writer, CsvEncoder, λ[a => CsvWriteConfig], CsvDataSourceWriter, CsvWriteDsl[
      A
    ]] {

  override protected val _writer = CsvDataSourceWriter

  override def config(config: CsvWriteConfig): CsvWriteDsl[A] =
    new CsvWriteDsl[A](config)

  def option(naming: Naming): CsvWriteDsl[A] =
    config(_config.copy(naming = naming))
}
