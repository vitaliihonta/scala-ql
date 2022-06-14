package scalaql.csv

import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv.CSVWriter
import scalaql.SideEffect
import scalaql.sources.DataSourceReadSupport
import scalaql.sources.DataSourceSupport
import scalaql.sources.DataSourceWriteSupport

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

trait ScalaqlCsvSupport extends DataSourceSupport[CsvDecoder.Row, CsvEncoder.Row, CsvConfig] {

  final object read extends DataSourceReadSupport[CsvDecoder.Row, CsvConfig] {
    override def read[A: CsvDecoder.Row](reader: => Reader)(implicit config: CsvConfig): Iterable[A] = {
      val csvReader = CSVReader.open(reader)(config.toTototoshi)

      try
        csvReader.iteratorWithHeaders
          .map(raw => implicitly[CsvDecoder.Row[A]].read(CsvEntry.Row(raw)))
          .toList
      finally
        reader.close()
    }
  }

  final object write extends DataSourceWriteSupport[CsvEncoder.Row, CsvConfig] {

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
      )
  }
}
