package scalaql.csv.internal

import com.github.tototoshi.csv.CSVWriter
import scalaql.SideEffect
import scalaql.csv.{CsvEncoder, CsvWriteConfig, CsvWriteContext}
import scalaql.sources.{DataSourceJavaIOWriter, DataSourceJavaIOWriterFilesSupport}
import java.io.Writer

object CsvDataSourceWriter extends CsvDataSourceWriter

class CsvDataSourceWriter
    extends DataSourceJavaIOWriter[CsvEncoder, λ[a => CsvWriteConfig]]
    with DataSourceJavaIOWriterFilesSupport[CsvEncoder, λ[a => CsvWriteConfig]] {

  override def write[A: CsvEncoder](writer: => Writer)(implicit config: CsvWriteConfig): SideEffect[Writer, ?, A] = {
    implicit val initialContext: CsvWriteContext = CsvWriteContext.initial(
      headers = CsvEncoder[A].headers,
      naming = config.naming
    )
    val headersWithNaming = initialContext.headers.map(config.naming(_))
    SideEffect[Writer, Boolean, A](
      initialState = false,
      acquire = () => writer,
      release = (writer, _) => writer.close(),
      (writer, writtenHeaders, value) => {
        val csvWriter = new CSVWriter(writer)(config.toTototoshi)
        if (!writtenHeaders) {
          csvWriter.writeRow(headersWithNaming)
        }
        val result = CsvEncoder[A].write(value)
        csvWriter.writeRow(alignWithHeaders(headersWithNaming, result))
        true
      }
    ).afterAll((writer, _) => writer.flush())
  }

  private def alignWithHeaders(headers: List[String], values: Map[String, String]): List[String] =
    headers.map { header =>
      values.getOrElse(header, "")
    }
}
