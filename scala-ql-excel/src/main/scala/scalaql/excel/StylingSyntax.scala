package scalaql.excel

import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Workbook
import scalaql.utils.GenericMutableConfigurator

trait StylingSyntax {
  final type Styling = (Workbook, CellStyle) => Unit

  final def cellStyle: GenericMutableConfigurator[Workbook, CellStyle] =
    GenericMutableConfigurator.withContext[Workbook, CellStyle]
}
