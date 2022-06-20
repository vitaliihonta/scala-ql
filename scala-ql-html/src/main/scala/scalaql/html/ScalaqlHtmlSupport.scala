package scalaql.html

import scalaql.SideEffect
import scalaql.sources.*
import scalatags.Text.all.*
import java.io.Writer

trait ScalaqlHtmlSupport extends DataSourceJavaIOWriteSupport[HtmlTableEncoder, HtmlTableEncoderConfig] {
  final object write
      extends DataSourceJavaIOWriter[HtmlTableEncoder, HtmlTableEncoderConfig]
      with DataSourceJavaIOWriterFilesSupport[HtmlTableEncoder, HtmlTableEncoderConfig] {

    override def write[A: HtmlTableEncoder](
      sink:            => Writer
    )(implicit config: HtmlTableEncoderConfig[A]
    ): SideEffect[?, ?, A] = {
      implicit val initialContext: HtmlTableEncoderContext = HtmlTableEncoderContext.initial(
        headers = HtmlTableEncoder[A].headers,
        fieldStyles = config.styling.fieldStyle
      )
      println(s"Headers: ${initialContext.headers}")
      SideEffect[Writer, List[Modifier], A](
        initialState = List.empty[Modifier],
        acquire = () => sink,
        release = { (writer, rows) =>
          val headers = tr(
            initialContext.headers
              .map(h => th(config.styling.headerStyle(h))(h))
          )
          val document = html(
            config.headTag,
            config.bodyTag(
              config.tableTag(
                headers,
                rows.reverse
              )
            )
          )
          document.writeTo(writer)
          writer.flush()
          writer.close()
        },
        use = { (_, rows, value) =>
          val row = HtmlTableEncoder[A].write(value).value
          row :: rows
        }
      )
    }
  }
}
