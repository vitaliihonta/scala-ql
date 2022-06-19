package scalaql.excel

import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.Sheet

case class ExcelConfig(evaluateFormulas: Boolean, choseWorksheet: Workbook => Sheet)

object ExcelConfig {
  implicit val default: ExcelConfig = ExcelConfig(
    evaluateFormulas = false,
    choseWorksheet = _.getSheetAt(0)
  )
}
