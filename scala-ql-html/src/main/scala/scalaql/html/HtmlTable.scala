package scalaql.html

import scalaql.sources.columnar.{TableApi, TableRowApi}
import scalatags.Text.all.Modifier
import scala.collection.mutable

class HtmlTableRow(cells: mutable.ListBuffer[(String, Modifier)]) extends TableRowApi[Modifier] {
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

object HtmlTableRow {
  def empty: HtmlTableRow = new HtmlTableRow(mutable.ListBuffer.empty)
}

class HtmlTable(rows: mutable.ListBuffer[HtmlTableRow]) extends TableApi[Modifier, HtmlTableRow] {
  override def prepend(row: HtmlTableRow): this.type = {
    rows.prepend(row)
    this
  }

  override def append(row: HtmlTableRow): this.type = {
    rows.append(row)
    this
  }

  override def currentRow: HtmlTableRow =
    rows.last

  override def getRows: List[List[(String, Modifier)]] = rows.toList.map(_.getCells)

  override def foreachRow(f: HtmlTableRow => Unit): Unit =
    rows.foreach(f)
}

object HtmlTable {
  def empty: HtmlTable = new HtmlTable(mutable.ListBuffer.empty)
}
