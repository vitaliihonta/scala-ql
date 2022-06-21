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
        fieldStyles = config.styling.fieldStyle,
        trTag = config.trTag,
        thTag = config.thTag,
        tdTag = config.tdTag
      )
      SideEffect[Writer, Table, A](
        initialState = Table.empty,
        acquire = () => sink,
        release = { (writer, table) =>
          val headers = {
            val row = TableRow.empty
            initialContext.headers.foreach { h =>
              row.append(h, config.thTag(config.styling.headerStyle(h))(config.naming(h)))
            }
            row
          }
          table.prepend(headers)
          fillGapsIntoTable(table)
          val document = config.htmlTag(
            config.headTag,
            config.bodyTag(
              config.tableTag(
                table.getRows
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
          HtmlTableEncoder[A].write(value, into.append(TableRow.empty))
          into
        }
      )
    }

    private def fillGapsIntoTable(
      table:                 Table
    )(implicit writeContext: HtmlTableEncoderContext
    ): Unit =
      table.foreachRow { row =>
        val resultHeaders = row.getFieldNames
        writeContext.headers.zipWithIndex.foreach { case (h, idx) =>
          if (!resultHeaders.contains(h)) {
            row.insert(idx, h, td(writeContext.fieldStyles(h)))
          }
        }
      }
  }
}
