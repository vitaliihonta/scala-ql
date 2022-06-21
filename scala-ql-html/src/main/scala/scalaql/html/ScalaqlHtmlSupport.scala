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
        rowTag = config.rowTag
      )
      SideEffect[Writer, Table, A](
        initialState = Table.empty,
        acquire = () => sink,
        release = { (writer, table) =>
          val headers = {
            val row = TableRow.empty
            initialContext.headers.foreach { h =>
              row.append(h, th(config.styling.headerStyle(h))(h))
            }
            row
          }
          table.prepend(headers)
          fillGapsIntoTable(table)
          val document = html(
            config.headTag,
            config.bodyTag(
              config.tableTag(
                table.getRows
                  .map { row =>
                    config.rowTag(row.map { case (_, mod) => mod })
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

    // TODO: remove
    private def fillGapsWithTd(
      result:                List[(String, Modifier)]
    )(implicit writeContext: HtmlTableEncoderContext
    ): List[(String, Modifier)] = {
      val resultHeaders = result.map(_._1).toSet

      orderedValues {
        val fillments = writeContext.headers.zipWithIndex.flatMap { case (h, offset) =>
          if (!resultHeaders.contains(h))
            Some(
              (h, offset, td(writeContext.fieldStyles(h)))
            )
          else None
        }

        val fromResults = result.zipWithIndex
          .map { case ((h, v), idx) => (h, idx, v) }

        fillments ++ fromResults
      }
    }

    private def orderedValues(
      result:                List[(String, Int, Modifier)]
    )(implicit writeContext: HtmlTableEncoderContext
    ): List[(String, Modifier)] = {
      val headersOrder = writeContext.headers.zipWithIndex.toMap
      result
        .sortBy { case (n, offset, _) => headersOrder.getOrElse(n, offset) }
        .map { case (h, _, v) => h -> v }
    }
  }
}
