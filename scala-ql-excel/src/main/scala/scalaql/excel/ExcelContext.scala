package scalaql.excel

import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.Workbook

sealed trait ExcelContext {
  def workbook: Workbook
  def path: List[String]

  lazy val formulaEvaluator: FormulaEvaluator = workbook.getCreationHelper.createFormulaEvaluator

  def startOffset: Int
}

case class ExcelReadContext(
  workbook:               Workbook,
  evaluateFormulas:       Boolean,
  headers:                Map[String, Int],
  cellResolutionStrategy: CellResolutionStrategy,
  path:                   List[String],
  currentOffset:          Int)
    extends ExcelContext {

  override def startOffset: Int =
    cellResolutionStrategy.getStartOffset(headers, path.head, currentOffset)

  def cannotDecodeError(cause: String): IllegalArgumentException =
    cellResolutionStrategy.cannotDecodeError(path, currentOffset, cause)
}

case class ExcelWriteContext(
  workbook:    Workbook,
  path:        List[String],
  startOffset: Int)
    extends ExcelContext
