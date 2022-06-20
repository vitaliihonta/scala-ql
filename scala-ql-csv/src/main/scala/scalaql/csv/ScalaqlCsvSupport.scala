package scalaql.csv

import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv.CSVWriter
import scalaql.SideEffect
import scalaql.sources.*
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
import java.io.Writer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import scala.collection.mutable

trait ScalaqlCsvSupport extends DataSourceJavaIOSupport[CsvDecoder, CsvEncoder, λ[a => CsvConfig], λ[a => CsvConfig]] {

  final object read
      extends DataSourceJavaIOReader[CsvDecoder, λ[a => CsvConfig]]
      with DataSourceJavaIOReaderFilesSupport[CsvDecoder, λ[a => CsvConfig]] {

    override protected def readImpl[A: CsvDecoder](reader: Reader)(implicit config: CsvConfig): Iterable[A] = {
      implicit val initialContext: CsvContext = CsvContext(path = Nil, naming = config.naming)
      CSVReader
        .open(reader)(config.toTototoshi)
        .iteratorWithHeaders
        .map(CsvDecoder[A].read(_).fold[A](throw _, identity[A]))
        .toList
    }
  }

  final object write
      extends DataSourceJavaIOWriter[CsvEncoder, λ[a => CsvConfig]]
      with DataSourceJavaIOWriterFilesSupport[CsvEncoder, λ[a => CsvConfig]] {

    override def write[A: CsvEncoder](writer: => Writer)(implicit config: CsvConfig): SideEffect[?, ?, A] = {
      implicit val initialContext: CsvContext = CsvContext(path = Nil, naming = config.naming)
      SideEffect[CSVWriter, Boolean, A](
        initialState = false,
        acquire = () => CSVWriter.open(writer)(config.toTototoshi),
        release = (writer, _) => writer.close(),
        (writer, writtenHeaders, value) => {
          if (!writtenHeaders) {
            writer.writeRow(CsvEncoder[A].headers)
          }
          val result = CsvEncoder[A].write(value)
          writer.writeRow(result.values.toList)
          true
        }
      ).afterAll((writer, _) => writer.flush())
    }
  }
}
