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

trait ScalaqlCsvSupport
    extends DataSourceJavaIOSupport[CsvDecoder.Row, CsvEncoder.Row, λ[a => CsvConfig], λ[a => CsvConfig]] {

  final object read
      extends DataSourceJavaIOReader[CsvDecoder.Row, λ[a => CsvConfig]]
      with DataSourceJavaIOReaderFilesSupport[CsvDecoder.Row, λ[a => CsvConfig]] {
    override protected def readImpl[A: CsvDecoder.Row](reader: Reader)(implicit config: CsvConfig): Iterable[A] =
      CSVReader
        .open(reader)(config.toTototoshi)
        .iteratorWithHeaders
        .map(raw => implicitly[CsvDecoder.Row[A]].read(CsvEntry.Row(raw)))
        .toList
  }

  final object write
      extends DataSourceJavaIOWriter[CsvEncoder.Row, λ[a => CsvConfig]]
      with DataSourceJavaIOWriterFilesSupport[CsvEncoder.Row, λ[a => CsvConfig]] {

    override def write[A: CsvEncoder.Row](writer: => Writer)(implicit config: CsvConfig): SideEffect[?, ?, A] =
      SideEffect[CSVWriter, Boolean, A](
        initialState = false,
        acquire = () => CSVWriter.open(writer)(config.toTototoshi),
        release = (writer, _) => writer.close(),
        (writer, writtenHeaders, value) => {
          val result = implicitly[CsvEncoder.Row[A]].write(value)
          if (!writtenHeaders) {
            writer.writeRow(result.row.keys.toList)
          }
          writer.writeRow(result.row.values.toList)
          true
        }
      ).afterAll((writer, _) => writer.flush())
  }
}
