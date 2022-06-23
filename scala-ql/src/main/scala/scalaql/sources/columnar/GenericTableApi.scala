package scalaql.sources.columnar

import scala.collection.mutable

class GenericTableRow[Cell](cells: mutable.ListBuffer[(String, Cell)]) extends TableRowApi[Cell, Cell] {
  override def append(name: String, value: Cell): this.type = {
    cells.append(name -> value)
    this
  }

  override def insert(idx: Int, name: String, value: Cell): this.type = {
    cells.insert(idx, name -> value)
    this
  }

  override def getFieldNames: Set[String] = cells.map { case (n, _) => n }.toSet

  override def getCells: List[(String, Cell)] = cells.toList
}

class GenericTableApi[Cell](rows: mutable.ListBuffer[GenericTableRow[Cell]])
    extends TableApi[Cell, Cell, GenericTableRow[Cell], GenericTableRow[Cell]] {

  override def prependEmptyRow: GenericTableRow[Cell] = {
    val row = new GenericTableRow[Cell](mutable.ListBuffer.empty)
    prepend(row)
    row
  }

  override def appendEmptyRow: this.type =
    append(new GenericTableRow[Cell](mutable.ListBuffer.empty))

  override def prepend(row: GenericTableRow[Cell]): this.type = {
    rows.prepend(row)
    this
  }

  override def append(row: GenericTableRow[Cell]): this.type = {
    rows.append(row)
    this
  }

  override def getRows: List[GenericTableRow[Cell]] = rows.toList

  override def currentRow: GenericTableRow[Cell] =
    rows.last

  def getRowValues: List[List[(String, Cell)]] = rows.toList.map(_.getCells)
}

object GenericTableApi {
  def empty[Cell]: GenericTableApi[Cell] = new GenericTableApi(mutable.ListBuffer.empty)
}
