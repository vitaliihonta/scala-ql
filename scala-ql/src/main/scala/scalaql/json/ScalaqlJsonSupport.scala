package scalaql.json

import io.circe.parser
import io.circe.Decoder
import io.circe.Encoder
import scalaql.SideEffectWithResource
import scalaql.syntax.DataSourceReadSupport
import scalaql.syntax.DataSourceSupport
import scalaql.syntax.DataSourceWriteSupport
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
      encoding:        Charset = StandardCharsets.UTF_8
    )(implicit config: JsonConfig = JsonConfig.default
    ): Iterable[A] =
      readFromSource {
        Source.fromFile(path.toFile, encoding.name)
      }

    override def string[A: Decoder](
      content:         String
    )(implicit config: JsonConfig = JsonConfig.default
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
    )(implicit config: JsonConfig = JsonConfig.default
    ): SideEffectWithResource[?, ?, A] = writeInto {
      Files.newBufferedWriter(path, encoding, openOptions: _*)
    }.afterAll((writer, _) => writer.flush())

    override def string[A: Encoder](
      builder:         mutable.StringBuilder
    )(implicit config: JsonConfig = JsonConfig.default
    ): SideEffectWithResource[?, ?, A] = {
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
    ): SideEffectWithResource[Writer, ?, A] = {
      val basics = SideEffectWithResource.stateless[Writer, A](
        acquire = () => acquireWriter,
        release = _.close(),
        use = (writer, value) => {
          writer.write(Encoder[A].apply(value).noSpaces)
          writer.write("\r\n")
        }
      )

      if (config.multiline) basics
      else basics.beforeAll(_.write("[")).afterAll((writer, _) => writer.write("]"))
    }
  }
}
