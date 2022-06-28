package scalaql.json.internal

import io.circe.{Encoder, Json, Printer}
import scalaql.SideEffect
import scalaql.json.JsonWriteConfig
import scalaql.sources.{DataSourceJavaIOWriter, DataSourceJavaIOWriterFilesSupport}
import java.io.Writer

object JsonDataSourceWriter extends JsonDataSourceWriter

class JsonDataSourceWriter
    extends DataSourceJavaIOWriter[Encoder, λ[a => JsonWriteConfig]]
    with DataSourceJavaIOWriterFilesSupport[Encoder, λ[a => JsonWriteConfig]] {

  override def write[A: Encoder](writer: => Writer)(implicit config: JsonWriteConfig): SideEffect[Writer, ?, A] = {
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
        if (isFirstRow) {
          writer.write(s"[${config.lineTerminator}")
        } else {
          writer.write(s",${config.lineTerminator}")
        }
        writer.write(printer.print(json))
      }.afterAll((writer, _) => writer.write(s"${config.lineTerminator}]"))
    }
    result.afterAll((writer, _) => writer.flush())
  }
}
