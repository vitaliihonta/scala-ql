package scalaql.csv

import com.github.tototoshi.csv.CSVReader

import java.io.StringReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path

trait ScalaqlCsvSupport {

  final def apply[A: CsvDecoder](
    path:               Path,
    encoding:           Charset = StandardCharsets.UTF_8
  )(implicit csvConfig: CsvConfig = CsvConfig.default
  ): Iterable[A] =
    readFromReader {
      CSVReader.open(path.toFile, encoding.name)(csvConfig.toTototoshi)
    }

  final def fromString[A: CsvDecoder](content: String)(implicit csvConfig: CsvConfig = CsvConfig.default): Iterable[A] =
    readFromReader {
      CSVReader.open(new StringReader(content))(csvConfig.toTototoshi)
    }

  private def readFromReader[A: CsvDecoder](reader: CSVReader): Iterable[A] =
    reader.iteratorWithHeaders
      .map(implicitly[CsvDecoder[A]].read)
      .toList
}
