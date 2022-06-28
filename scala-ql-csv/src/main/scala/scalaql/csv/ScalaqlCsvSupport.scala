package scalaql.csv

import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv.CSVWriter
import scalaql.SideEffect
import scalaql.sources.*
import java.io.Reader
import java.io.Writer

trait ScalaqlCsvSupport
    extends DataSourceJavaIOSupport[CsvDecoder, CsvEncoder, λ[a => CsvReadConfig], λ[a => CsvWriteConfig]] {

  final object read
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

  final object write
      extends DataSourceJavaIOWriter[CsvEncoder, λ[a => CsvWriteConfig]]
      with DataSourceJavaIOWriterFilesSupport[CsvEncoder, λ[a => CsvWriteConfig]] {

    override def write[A: CsvEncoder](writer: => Writer)(implicit config: CsvWriteConfig): SideEffect[?, ?, A] = {
      implicit val initialContext: CsvWriteContext = CsvWriteContext.initial(
        headers = CsvEncoder[A].headers,
        naming = config.naming
      )
      val headersWithNaming = initialContext.headers.map(config.naming)
      SideEffect[CSVWriter, Boolean, A](
        initialState = false,
        acquire = () => CSVWriter.open(writer)(config.toTototoshi),
        release = (writer, _) => writer.close(),
        (writer, writtenHeaders, value) => {
          if (!writtenHeaders) {
            writer.writeRow(headersWithNaming)
          }
          val result = CsvEncoder[A].write(value)
          writer.writeRow(alignWithHeaders(headersWithNaming, result))
          true
        }
      ).afterAll((writer, _) => writer.flush())
    }

    private def alignWithHeaders(headers: List[String], values: Map[String, String]): List[String] =
      headers.map { header =>
        values.getOrElse(header, "")
      }
  }
}
