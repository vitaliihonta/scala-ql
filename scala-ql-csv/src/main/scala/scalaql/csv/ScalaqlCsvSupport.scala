package scalaql.csv

import scalaql.csv.internal.{CsvDataSourceReader, CsvDataSourceWriter}
import scalaql.sources.*

trait ScalaqlCsvSupport
    extends DataSourceJavaIOSupport[
      CsvDecoder,
      CsvEncoder,
      λ[a => CsvReadConfig],
      λ[a => CsvWriteConfig],
      CsvDataSourceReader,
      CsvDataSourceWriter,
      CsvReadDsl,
      CsvWriteDsl
    ] {

  override def read[A]: CsvReadDsl[A]   = new CsvReadDsl[A](CsvReadConfig.default)
  override def write[A]: CsvWriteDsl[A] = new CsvWriteDsl[A](CsvWriteConfig.default)
}
