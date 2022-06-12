package scalaql.json

import scalaql.SideEffect
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.parser
import scalaql.sources.DataSourceReadSupport
import scalaql.sources.DataSourceSupport
import scalaql.sources.DataSourceWriteSupport
import java.io.StringWriter
import java.io.Writer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import scala.collection.mutable
import scala.io.Source

trait ScalaqlJsonSupport extends DataSourceSupport[Decoder, Encoder, JsonConfig] {

  final object read extends DataSourceReadSupport[Decoder, JsonConfig] {
    override def file[A: Decoder](
      path:            Path,
      encoding:        Charset
    )(implicit config: JsonConfig
    ): Iterable[A] =
      readFromSource {
        Source.fromFile(path.toFile, encoding.name)
      }

    override def string[A: Decoder](
      content:         String
    )(implicit config: JsonConfig
    ): Iterable[A] =
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

  final object write extends DataSourceWriteSupport[Encoder, JsonConfig] {

    override def file[A: Encoder](
      path:            Path,
      encoding:        Charset,
      openOptions:     OpenOption*
    )(implicit config: JsonConfig
    ): SideEffect[?, ?, A] = writeInto {
      Files.newBufferedWriter(path, encoding, openOptions: _*)
    }.afterAll((writer, _) => writer.flush())

    override def string[A: Encoder](
      builder:         mutable.StringBuilder
    )(implicit config: JsonConfig
    ): SideEffect[?, ?, A] = {
      val writer = new StringWriter()
      writeInto {
        writer
      } onExit {
        builder.append(writer)
      }
    }

    private def writeInto[A: Encoder](
      acquireWriter:   => Writer
    )(implicit config: JsonConfig
    ): SideEffect[Writer, ?, A] = {
      def basics(writeLine: (Writer, Boolean, Json) => Unit) = SideEffect[Writer, Boolean, A](
        initialState = true,
        acquire = () => acquireWriter,
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
        }
          .beforeAll(_.write("[\r\n"))
          .afterAll((writer, _) => writer.write("\r\n]"))
      }
    }
  }
}
