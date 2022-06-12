package scalaql.csv

import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv.CSVWriter
import scalaql.SideEffectWithResource
import scalaql.sources.DataSourceReadSupport
import scalaql.sources.DataSourceSupport
import scalaql.sources.DataSourceWriteSupport
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import scala.collection.mutable

trait ScalaqlCsvSupport extends DataSourceSupport[CsvDecoder.Row, CsvEncoder.Row, CsvConfig] {

  final object read extends DataSourceReadSupport[CsvDecoder.Row, CsvConfig] {
    override def file[A: CsvDecoder.Row](
      path:               Path,
      encoding:           Charset
    )(implicit csvConfig: CsvConfig
    ): Iterable[A] =
      readFrom {
        CSVReader.open(path.toFile, encoding.name)(csvConfig.toTototoshi)
      }

    override def string[A: CsvDecoder.Row](
      content:         String
    )(implicit config: CsvConfig
    ): Iterable[A] =
      readFrom {
        CSVReader.open(new StringReader(content))(config.toTototoshi)
      }

    private def readFrom[A: CsvDecoder.Row](reader: CSVReader): Iterable[A] =
      reader.iteratorWithHeaders
        .map(raw => implicitly[CsvDecoder.Row[A]].read(CsvEntry.Row(raw)))
        .toList
  }

  final object write extends DataSourceWriteSupport[CsvEncoder.Row, CsvConfig] {

    override def file[A: CsvEncoder.Row](
      path:            Path,
      encoding:        Charset,
      openOptions:     OpenOption*
    )(implicit config: CsvConfig
    ): SideEffectWithResource[?, ?, A] =
      writeInto {
        val writer = Files.newBufferedWriter(path, encoding, openOptions: _*)
        new CSVWriter(writer)(config.toTototoshi)
      }.afterAll((writer, _) => writer.flush())

    override def string[A: CsvEncoder.Row](
      builder:         mutable.StringBuilder
    )(implicit config: CsvConfig
    ): SideEffectWithResource[?, ?, A] = {
      val writer = new StringWriter()
      writeInto {
        CSVWriter.open(writer)(config.toTototoshi)
      } onExit {
        builder.append(writer)
      }
    }

    private def writeInto[A: CsvEncoder.Row](
      acquireWriter: => CSVWriter
    ): SideEffectWithResource[CSVWriter, Boolean, A] =
      SideEffectWithResource[CSVWriter, Boolean, A](
        initialState = false,
        acquire = () => acquireWriter,
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
