package scalaql.sources.columnar

import scalaql.sources.Naming

trait TableRowApi[Cell] {
  def append(name: String, cell: Cell): this.type

  def insert(idx: Int, name: String, value: Cell): this.type

  def getFieldNames: Set[String]

  def getCells: List[(String, Cell)]
}

trait TableApi[Cell, Row <: TableRowApi[Cell]] {
  def prepend(row: Row): this.type

  def append(row: Row): this.type

  def currentRow: Row

  def getRows: List[List[(String, Cell)]]

  def foreachRow(f: Row => Unit): Unit
}

trait TableApiContext[Self <: TableApiContext[Self]] {
  def location: CodecPath

  def enterField(name: String): Self

  def enterIndex(idx: Int): Self

  private[scalaql] def fieldLocation: CodecPath.AtField =
    location.fieldLocation
}

trait TableApiWriteContext[Self <: TableApiWriteContext[Self]] extends TableApiContext[Self] {
  def headers: List[String]
}

object TableApiFunctions {
  def fillGapsIntoTable[Cell, Row <: TableRowApi[Cell], Table <: TableApi[Cell, Row]](
    table:        Table
  )(fill:         String => Cell
  )(implicit ctx: TableApiWriteContext[?]
  ): Unit = table.foreachRow { row =>
    val resultHeaders = row.getFieldNames
    ctx.headers.zipWithIndex.foreach { case (h, idx) =>
      if (!resultHeaders.contains(h)) {
        row.insert(idx, h, fill(h))
      }
    }
  }
}
