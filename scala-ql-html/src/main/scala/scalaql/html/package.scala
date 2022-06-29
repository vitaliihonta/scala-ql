package scalaql

import scalaql.sources.columnar.{GenericTableRow, GenericTableApi}
import scalatags.Text.Modifier

package object html extends ScalaqlHtmlSupport {
  type HtmlTableRow = GenericTableRow[Modifier]
  type HtmlTable    = GenericTableApi[Modifier]
  object HtmlTable {
    def empty: HtmlTable = GenericTableApi.empty[Modifier]
  }
}
