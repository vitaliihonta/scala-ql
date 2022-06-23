package scalaql.visualization

import scalaql.sources.columnar.{TableApi, TableRowApi}
import scala.collection.mutable

class ShowTableRow(cells: mutable.ListBuffer[(String, String)]) extends TableRowApi[String, String] {
  override def append(name: String, value: String): this.type = {
    cells.append(name -> value)
    this
  }

  override def insert(idx: Int, name: String, value: String): this.type = {
    cells.insert(idx, name -> value)
    this
  }

  override def getFieldNames: Set[String] = cells.map { case (n, _) => n }.toSet

  override def getCells: List[(String, String)] = cells.toList
}

class ShowTable(rows: mutable.ListBuffer[ShowTableRow]) extends TableApi[String, String, ShowTableRow, ShowTableRow] {

  override def prependEmptyRow: ShowTableRow = {
    val row = new ShowTableRow(mutable.ListBuffer.empty)
    prepend(row)
    row
  }

  override def appendEmptyRow: this.type =
    append(new ShowTableRow(mutable.ListBuffer.empty))

  override def prepend(row: ShowTableRow): this.type = {
    rows.prepend(row)
    this
  }

  override def append(row: ShowTableRow): this.type = {
    rows.append(row)
    this
  }

  override def getRows: List[ShowTableRow] = rows.toList

  override def currentRow: ShowTableRow =
    rows.last

  def getRowValues: List[List[(String, String)]] = rows.toList.map(_.getCells)
}

object ShowTable {
  def empty: ShowTable = new ShowTable(mutable.ListBuffer.empty)
}
