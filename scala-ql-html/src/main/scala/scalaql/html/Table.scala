package scalaql.html

import scalatags.Text.all.Modifier
import scala.collection.mutable

class TableRow(cells: mutable.ListBuffer[(String, Modifier)]) {
  def append(name: String, value: Modifier): this.type = {
    cells.append(name -> value)
    this
  }

  def insert(idx: Int, name: String, value: Modifier): this.type = {
    cells.insert(idx, name -> value)
    this
  }

  def getFieldNames: Set[String] = cells.map { case (n, _) => n }.toSet

  def getCells: List[(String, Modifier)] = cells.toList
}

object TableRow {
  def empty: TableRow = new TableRow(mutable.ListBuffer.empty)
}

class Table(rows: mutable.ListBuffer[TableRow]) {
  def prepend(row: TableRow): this.type = {
    rows.prepend(row)
    this
  }

  def append(row: TableRow): this.type = {
    rows.append(row)
    this
  }

  def currentRow: TableRow =
    rows.last

  def getRows: List[List[(String, Modifier)]] = rows.toList.map(_.getCells)

  def foreachRow(f: TableRow => Unit): Unit =
    rows.foreach(f)
}

object Table {
  def empty: Table = new Table(mutable.ListBuffer.empty)
}
