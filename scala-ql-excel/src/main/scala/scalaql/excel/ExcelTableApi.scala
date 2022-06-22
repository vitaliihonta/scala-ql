package scalaql.excel

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.{Workbook, Sheet}
import scalaql.sources.columnar.{TableRowApi, TableApi, TableApiWriteContext}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

object ExcelTableApi {
  type ConfigureCell = Cell => Cell
  type ConfigureRow  = ExcelTableRowApi => ExcelTableRowApi
}

import ExcelTableApi.*

class ExcelTableRowApi(row: Row, nameToCell: mutable.Map[String, Int]) extends TableRowApi[Cell, ConfigureCell] {
  override def append(name: String, configure: ConfigureCell): this.type = {
    val idx  = row.getLastCellNum + 1
    val cell = row.createCell(idx)
    configure(cell)
    nameToCell += (name -> idx)
    this
  }

  override def insert(idx: Int, name: String, configure: ConfigureCell): this.type = {
    val cell = row.createCell(idx)
    configure(cell)
    nameToCell += (name -> idx)
    this
  }

  override def getFieldNames: Set[String] = nameToCell.keySet.toSet

  override def getCells: List[(String, Cell)] =
    nameToCell.map { case (name, idx) => name -> row.getCell(idx) }.toList
}

class ExcelTableApi(sheet: Sheet, headers: List[String])
    extends TableApi[Cell, ConfigureCell, ExcelTableRowApi, ConfigureRow] {

  override def prependEmptyRow: ExcelTableRowApi = {
    prepend(identity[ExcelTableRowApi])
    rowToApi(sheet.getRow(0))
  }

  override def appendEmptyRow: this.type =
    append(identity[ExcelTableRowApi])

  override def prepend(configure: ConfigureRow): this.type = {
    val row = sheet.createRow(0)
    configure(rowToApi(row))
    this
  }

  override def append(configure: ConfigureRow): this.type = {
    val idx = sheet.getLastRowNum + 1
    val row = sheet.createRow(idx)
    configure(rowToApi(row))
    this
  }

  override def currentRow: ExcelTableRowApi = {
    val idx = sheet.getLastRowNum
    rowToApi(sheet.getRow(idx))
  }

  override def getRows: List[ExcelTableRowApi] = sheet
    .rowIterator()
    .asScala
    .map(rowToApi)
    .toList

  private def rowToApi(row: Row): ExcelTableRowApi = {
    val nameToCell = mutable.Map(headers.zipWithIndex: _*)
    new ExcelTableRowApi(row, nameToCell)
  }
}
