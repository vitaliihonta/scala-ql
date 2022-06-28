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

object ExcelStyling {

  def builder[A]: ExcelStylingBuilder[A] = new ExcelStylingBuilder[A]()

  final class Configured[A](
    header: String => Option[Styling],
    cell:   String => Option[Styling])
      extends ExcelStyling[A] {

    override def headerStyle(name: String): Option[Styling] = header(name)
    override def cellStyle(name: String): Option[Styling]   = cell(name)
  }

  final case object NoStyling extends ExcelStyling[Any] {
    override def headerStyle(name: String): Option[Styling] = None
    override def cellStyle(name: String): Option[Styling]   = None
  }
}
