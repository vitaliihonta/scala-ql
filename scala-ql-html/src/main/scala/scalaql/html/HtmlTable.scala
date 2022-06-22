package scalaql.html

import scalaql.sources.columnar.{TableApi, TableRowApi}
import scalatags.Text.all.Modifier
import scala.collection.mutable

class HtmlTableRow(cells: mutable.ListBuffer[(String, Modifier)]) extends TableRowApi[Modifier, Modifier] {
  override def append(name: String, value: Modifier): this.type = {
    cells.append(name -> value)
    this
  }

  override def insert(idx: Int, name: String, value: Modifier): this.type = {
    cells.insert(idx, name -> value)
    this
  }

  override def getFieldNames: Set[String] = cells.map { case (n, _) => n }.toSet

  override def getCells: List[(String, Modifier)] = cells.toList
}

class HtmlTable(rows: mutable.ListBuffer[HtmlTableRow])
    extends TableApi[Modifier, Modifier, HtmlTableRow, HtmlTableRow] {

  override def prependEmptyRow: HtmlTableRow = {
    val row = new HtmlTableRow(mutable.ListBuffer.empty)
    prepend(row)
    row
  }

  override def appendEmptyRow: this.type =
    append(new HtmlTableRow(mutable.ListBuffer.empty))

  override def prepend(row: HtmlTableRow): this.type = {
    rows.prepend(row)
    this
  }

  override def append(row: HtmlTableRow): this.type = {
    rows.append(row)
    this
  }

  override def getRows: List[HtmlTableRow] = rows.toList

  override def currentRow: HtmlTableRow =
    rows.last

  def getRowValues: List[List[(String, Modifier)]] = rows.toList.map(_.getCells)
}

object HtmlTable {
  def empty: HtmlTable = new HtmlTable(mutable.ListBuffer.empty)
}
