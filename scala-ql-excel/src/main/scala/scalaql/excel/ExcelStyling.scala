package scalaql.excel

import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.TableStyle

trait ExcelStyling[-A] {
  def cellStyles: Map[String, CellStyle => Unit]
}

object ExcelStyling extends LowPriorityExcelStyling {
  def apply[A](implicit ev: ExcelStyling[A]): ev.type = ev

  def builder[A]: ExcelStylingBuilder[A] = new ExcelStylingBuilder[A]()

  final class Configured[A](base: Map[String, CellStyle => Unit]) extends ExcelStyling[A] {
    private val noop: CellStyle => Unit = _ => {}

    override val cellStyles: Map[String, CellStyle => Unit] =
      base.withDefault(_ => noop)

    override def toString(): String = s"ExcelStyling.Configured(keys=${base.keySet})"
  }
}

trait LowPriorityExcelStyling {
  implicit val NoStyling: ExcelStyling[Any] = new ExcelStyling[Any] {
    override val cellStyles: Map[String, CellStyle => Unit] = Map.empty
  }
}
