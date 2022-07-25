package scalaql.csv.internal

import com.github.tototoshi.csv.CSVReader
import scalaql.csv.{CsvDecoder, CsvReadConfig, CsvReadContext}
import scalaql.sources.{DataSourceJavaIOReader, DataSourceJavaIOReaderFilesSupport, DataSourceJavaIOReaderHttpSupport}

import java.io.Reader

object CsvDataSourceReader extends CsvDataSourceReader

class CsvDataSourceReader
    extends DataSourceJavaIOReader[CsvDecoder, λ[a => CsvReadConfig]]
    with DataSourceJavaIOReaderFilesSupport[CsvDecoder, λ[a => CsvReadConfig]]
    with DataSourceJavaIOReaderHttpSupport[CsvDecoder, λ[a => CsvReadConfig]] {

  override protected def readImpl[A: CsvDecoder](reader: Reader)(implicit config: CsvReadConfig): Iterable[A] = {
    implicit val initialContext: CsvReadContext = CsvReadContext.initial(
      naming = config.naming,
      caseSensitive = config.caseSensitive,
      emptyStringInOptions = config.emptyStringInOptions
    )
    CSVReader
      .open(reader)(config.toTototoshi)
      .iteratorWithHeaders
      .flatMap { baseRow =>
        val row = baseRow.map { case (k, v) =>
          val base = k.trim
          (if (config.caseSensitive) base else base.toLowerCase) -> v
        }

        if (config.omitEmptyLines && row.isEmpty) None
        else {
          Some(CsvDecoder[A].read(row).fold[A](throw _, identity[A]))
        }
      }
      .toList
  }
}
