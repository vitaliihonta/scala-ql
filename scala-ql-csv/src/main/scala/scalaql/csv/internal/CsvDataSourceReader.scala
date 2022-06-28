package scalaql.csv.internal

import com.github.tototoshi.csv.CSVReader
import scalaql.csv.{CsvDecoder, CsvReadConfig, CsvReadContext}
import scalaql.sources.{DataSourceJavaIOReader, DataSourceJavaIOReaderFilesSupport}
import java.io.Reader

object CsvDataSourceReader extends CsvDataSourceReader

class CsvDataSourceReader
    extends DataSourceJavaIOReader[CsvDecoder, λ[a => CsvReadConfig]]
    with DataSourceJavaIOReaderFilesSupport[CsvDecoder, λ[a => CsvReadConfig]] {

  override protected def readImpl[A: CsvDecoder](reader: Reader)(implicit config: CsvReadConfig): Iterable[A] = {
    implicit val initialContext: CsvReadContext = CsvReadContext.initial(
      naming = config.naming,
      caseSensitive = config.caseSensitive
    )
    CSVReader
      .open(reader)(config.toTototoshi)
      .iteratorWithHeaders
      .map { baseRow =>
        val row =
          if (config.caseSensitive) baseRow
          else baseRow.map { case (k, v) => k.toLowerCase -> v }

        CsvDecoder[A].read(row).fold[A](throw _, identity[A])
      }
      .toList
  }
}
