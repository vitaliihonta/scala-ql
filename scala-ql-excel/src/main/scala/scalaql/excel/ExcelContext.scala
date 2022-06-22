package scalaql.excel

import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.Cell
import scalaql.sources.Naming
import scalaql.sources.columnar.{CodecPath, TableApiContext, TableApiWriteContext}

sealed trait ExcelContext {
  def workbook: Workbook
  def location: CodecPath

  lazy val formulaEvaluator: FormulaEvaluator = workbook.getCreationHelper.createFormulaEvaluator
}

case class ExcelReadContext(
  workbook:               Workbook,
  naming:                 Naming,
  evaluateFormulas:       Boolean,
  headers:                Map[String, Int],
  cellResolutionStrategy: CellResolutionStrategy,
  location:               CodecPath,
  currentOffset:          Int)
    extends TableApiContext[ExcelReadContext]
    with ExcelContext { self =>

  override def enterField(name: String): ExcelReadContext =
    copy(location = CodecPath.AtField(name, self.location))

  override def enterIndex(idx: Int): ExcelReadContext =
    copy(location = CodecPath.AtIndex(idx, self.fieldLocation))

  def startOffset: Either[ExcelDecoderException, Int] =
    cellResolutionStrategy
      .getStartOffset(headers, location, naming, currentOffset)
      .toRight(
        cellResolutionStrategy.unableToFindCell(location, currentOffset)
      )

  def cannotDecodeError(cause: String): ExcelDecoderException =
    cellResolutionStrategy.cannotDecodeError(location, currentOffset, cause)
}

case class ExcelWriteContext(
  workbook:    Workbook,
  headers:     List[String],
  location:    CodecPath,
  startOffset: Int,
  cellStyle:   String => Option[Styling])
    extends TableApiWriteContext[ExcelWriteContext]
    with ExcelContext { self =>

  override def enterField(name: String): ExcelWriteContext =
    copy(location = CodecPath.AtField(name, self.location))

  override def enterIndex(idx: Int): ExcelWriteContext =
    copy(location = CodecPath.AtIndex(idx, self.fieldLocation))

  def applyCellStyle(cell: Cell): Unit =
    cellStyle(location.fieldLocation.name).foreach { styling =>
      val style: CellStyle = workbook.createCellStyle()
      styling(cell.getSheet.getWorkbook, style)
      cell.setCellStyle(style)
    }
}

object ExcelWriteContext {
  def initial(workbook: Workbook, headers: List[String], cellStyle: String => Option[Styling]): ExcelWriteContext =
    ExcelWriteContext(
      workbook = workbook,
      headers = headers,
      location = CodecPath.Root,
      startOffset = 0,
      cellStyle = cellStyle
    )
}
