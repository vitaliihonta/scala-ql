package scalaql.html.internal

import scalaql.SideEffect
import scalaql.html.*
import scalaql.sources.*
import scalaql.sources.columnar.TableApiFunctions
import scalatags.Text.all.*
import java.io.Writer

object HtmlDataSourceWriter extends HtmlDataSourceWriter

class HtmlDataSourceWriter
    extends DataSourceJavaIOWriter[HtmlTableEncoder, HtmlTableEncoderConfig]
    with DataSourceJavaIOWriterFilesSupport[HtmlTableEncoder, HtmlTableEncoderConfig] {

  override def write[A: HtmlTableEncoder](
    sink:            => Writer
  )(implicit config: HtmlTableEncoderConfig[A]
  ): SideEffect[Writer, ?, A] = {
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
      release = { (writer, _) =>
        writer.flush()
        writer.close()
      },
      use = { (_, into, value) =>
        HtmlTableEncoder[A].write(value, into.appendEmptyRow)
        into
      }
    ).afterAll { (writer, table) =>
      writeHeaders(initialContext.headers, table)(config)
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
    }
  }

  private def writeHeaders(
    headers:         List[String],
    table:           HtmlTable
  )(implicit config: HtmlTableEncoderConfig[?]
  ): Unit = {
    val row = table.prependEmptyRow
    headers.foreach { h =>
      row.append(h, config.thTag(config.styling.headerStyle(h))(config.naming(h)))
    }
  }
}
