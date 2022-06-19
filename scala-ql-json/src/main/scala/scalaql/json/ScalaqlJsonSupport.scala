package scalaql.json

import scalaql.SideEffect
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.Printer
import io.circe.parser
import scalaql.sources.DataSourceReader
import scalaql.sources.DataSourceSupport
import scalaql.sources.DataSourceWriter

import scala.jdk.CollectionConverters.*
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

  final object read extends DataSourceReader[Decoder, JsonConfig] {
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

  final object write extends DataSourceWriter[Encoder, JsonConfig] {
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

      val result = if (config.multiline) {
        basics { (writer, _, json) =>
          writer.write(json.noSpaces)
          writer.write(config.lineTerminator)
        }
      } else {
        val printer = Printer(
          dropNullValues = false,
          indent = " " * 3,
          lbraceRight = config.lineTerminator,
          rbraceLeft = config.lineTerminator,
          lbracketRight = config.lineTerminator,
          rbracketLeft = config.lineTerminator,
          lrbracketsEmpty = config.lineTerminator,
          arrayCommaRight = config.lineTerminator,
          objectCommaRight = config.lineTerminator,
          colonLeft = " ",
          colonRight = " "
        )
        basics { (writer, isFirstRow, json) =>
          if (!isFirstRow) {
            writer.write(s",${config.lineTerminator}")
          }
          writer.write(printer.print(json))
        }.beforeAll(_.write(s"[${config.lineTerminator}"))
          .afterAll((writer, _) => writer.write(s"${config.lineTerminator}]"))
      }
      result.afterAll((writer, _) => writer.flush())
    }
  }
}
