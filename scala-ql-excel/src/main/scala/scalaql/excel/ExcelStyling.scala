package scalaql.excel

import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.TableStyle

trait ExcelStyling[-A] {
  def headerStyle(name: String): Option[Styling]
  def cellStyle(name:   String): Option[Styling]
}

object ExcelStyling extends LowPriorityExcelStyling {
  def apply[A](implicit ev: ExcelStyling[A]): ev.type = ev

  def builder[A]: ExcelStylingBuilder[A] = new ExcelStylingBuilder[A]()

  final class Configured[A](
    header: String => Option[Styling],
    cell:   String => Option[Styling])
      extends ExcelStyling[A] {

    override def headerStyle(name: String): Option[Styling] = header(name)
    override def cellStyle(name: String): Option[Styling]   = cell(name)
  }
}

trait LowPriorityExcelStyling {
  implicit lazy val NoStyling: ExcelStyling[Any] = new ExcelStyling[Any] {
    override def headerStyle(name: String): Option[Styling] = None
    override def cellStyle(name: String): Option[Styling]   = None
  }
}
