package scalaql.html

import scalaql.SideEffect
import scalaql.sources.*
import scalaql.sources.columnar.TableApiFunctions
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
        fieldStyles = config.styling.fieldStyle,
        trTag = config.trTag,
        thTag = config.thTag,
        tdTag = config.tdTag
      )
      SideEffect[Writer, HtmlTable, A](
        initialState = HtmlTable.empty,
        acquire = () => sink,
        release = { (writer, table) =>
          val headers = {
            val row = table.prependEmptyRow
            initialContext.headers.foreach { h =>
              row.append(h, config.thTag(config.styling.headerStyle(h))(config.naming(h)))
            }
            row
          }
          TableApiFunctions.fillGapsIntoTable[Modifier, Modifier, HtmlTableRow](table)(h =>
            td(initialContext.fieldStyles(h))
          )
          val document = config.htmlTag(
            config.headTag,
            config.bodyTag(
              config.tableTag(
                table.getRowValues
                  .map { row =>
                    config.trTag(row.map { case (_, mod) => mod })
                  }
              )
            )
          )
          document.writeTo(writer)
          writer.flush()
          writer.close()
        },
        use = { (_, into, value) =>
          HtmlTableEncoder[A].write(value, into.appendEmptyRow)
          into
        }
      )
    }
  }
}
