package scalaql.sources.columnar

trait TableRowApi[Cell, CellValue] extends Serializable {
  def append(name: String, cellValue: CellValue): this.type

  def insert(idx: Int, name: String, value: CellValue): this.type

  def getFieldNames: Set[String]

  def getCells: List[(String, Cell)]
}

trait TableApi[Cell, CellValue, Row <: TableRowApi[Cell, CellValue], RowValue] extends Serializable {
  def prependEmptyRow: Row

  def appendEmptyRow: this.type

  def prepend(row: RowValue): this.type

  def append(row: RowValue): this.type

  def currentRow: Row

  def getRows: List[Row]
}

trait TableApiContext[Self <: TableApiContext[Self]] extends Serializable {
  def location: CodecPath

  def enterField(name: String): Self

  def enterIndex(idx: Int): Self

  private[scalaql] def fieldLocation: CodecPath.AtField =
    location.fieldLocation
}

trait TableApiWriteContext[Self <: TableApiWriteContext[Self]] extends TableApiContext[Self] with Serializable {
  def headers: List[String]
}

object TableApiFunctions {
  def fillGapsIntoTable[Cell, CellValue, Row <: TableRowApi[Cell, CellValue]](
    table:        TableApi[Cell, CellValue, Row, ?]
  )(fill:         String => CellValue
  )(implicit ctx: TableApiWriteContext[?]
  ): Unit = table.getRows.foreach { row =>
    val resultHeaders = row.getFieldNames
    ctx.headers.zipWithIndex.foreach { case (h, idx) =>
      if (!resultHeaders.contains(h)) {
        row.insert(idx, h, fill(h))
      }
    }
  }
}
