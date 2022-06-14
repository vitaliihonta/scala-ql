package scalaql.json

import scalaql.SideEffect
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.parser
import scalaql.sources.DataSourceReadSupport
import scalaql.sources.DataSourceSupport
import scalaql.sources.DataSourceWriteSupport
import scala.jdk.CollectionConverters._
import java.io.BufferedReader
import java.io.Reader
import java.io.StringWriter
import java.io.Writer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.util.stream.Collectors
import scala.collection.mutable
import scala.io.Source

trait ScalaqlJsonSupport extends DataSourceSupport[Decoder, Encoder, JsonConfig] {

  final object read extends DataSourceReadSupport[Decoder, JsonConfig] {
    protected def readImpl[A: Decoder](reader: Reader)(implicit config: JsonConfig): Iterable[A] = {
      val bufferedReader = new BufferedReader(reader)
      if (config.multiline) {
        bufferedReader
          .lines()
          // java lambdas, need types
          .map[A]((line: String) => parser.decode[A](line).toTry.get)
          .collect(Collectors.toList[A])
          .asScala
          .toList
      } else {
        val content = bufferedReader
          .lines()
          .collect(Collectors.joining())

        parser.decode[List[A]](content).toTry.get
      }
    }
  }

  final object write extends DataSourceWriteSupport[Encoder, JsonConfig] {
    override def write[A: Encoder](writer: => Writer)(implicit config: JsonConfig): SideEffect[?, ?, A] = {
      def basics(writeLine: (Writer, Boolean, Json) => Unit) = SideEffect[Writer, Boolean, A](
        initialState = true,
        acquire = () => writer,
        release = (writer, _) => writer.close(),
        use = (writer, isFirstRow, value) => {
          writeLine(writer, isFirstRow, Encoder[A].apply(value))
          false
        }
      )

      if (config.multiline) {
        basics { (writer, _, json) =>
          writer.write(json.noSpaces)
          writer.write("\r\n")
        }
      } else {
        basics { (writer, isFirstRow, json) =>
          if (!isFirstRow) {
            writer.write(",")
          }
          writer.write(json.spaces2)
        }.beforeAll(_.write("[\r\n")).afterAll((writer, _) => writer.write("\r\n]"))
      }
    }
  }
}
