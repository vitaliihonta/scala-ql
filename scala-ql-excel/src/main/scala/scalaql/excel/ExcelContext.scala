package scalaql.excel

import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.Workbook

case class ExcelContext(
  workbook:               Workbook,
  evaluateFormulas:       Boolean,
  headers:                Map[String, Int],
  cellResolutionStrategy: CellResolutionStrategy,
  path:                   List[String],
  currentOffset:          Int) { self =>

  lazy val formulaEvaluator: FormulaEvaluator = workbook.getCreationHelper.createFormulaEvaluator

  def startOffset: Int =
    cellResolutionStrategy.getStartOffset(headers, path.head, currentOffset)

  def cannotDecodeError(cause: String): IllegalArgumentException =
    cellResolutionStrategy.cannotDecodeError(path, currentOffset, cause)
}
