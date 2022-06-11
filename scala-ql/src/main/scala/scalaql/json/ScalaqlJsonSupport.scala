package scalaql.json

import io.circe.parser
import io.circe.Decoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import scala.io.Source

trait ScalaqlJsonSupport {

  final def apply[A: Decoder](
    path:                Path,
    encoding:            Charset = StandardCharsets.UTF_8
  )(implicit jsonConfig: JsonConfig = JsonConfig.default
  ): Iterable[A] =
    readFromSource {
      Source.fromFile(path.toFile, encoding.name)
    }

  final def fromString[A: Decoder](content: String)(implicit jsonConfig: JsonConfig = JsonConfig.default): Iterable[A] =
    readFromSource {
      Source.fromString(content)
    }

  private def readFromSource[A: Decoder](reader: Source)(implicit jsonConfig: JsonConfig): Iterable[A] =
    if (jsonConfig.multiline) {
      reader
        .getLines()
        .map(parser.decode[A](_).toTry.get)
        .toList
    } else {
      parser.decode[List[A]](reader.mkString).toTry.get
    }
}
