package scalaql.excel

import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Cell
import scalaql.sources.Naming
import java.util.regex.Pattern

case class ExcelReadConfig(
  evaluateFormulas:       Boolean,
  choseWorksheet:         Workbook => Sheet,
  cellResolutionStrategy: CellResolutionStrategy,
  worksheetName:          Option[String])

object ExcelReadConfig extends LowPriorityExcelReadConfig {}

trait LowPriorityExcelReadConfig {
  implicit val default: ExcelReadConfig = ExcelReadConfig(
    evaluateFormulas = false,
    choseWorksheet = _.getSheetAt(0),
    cellResolutionStrategy = CellResolutionStrategy.IndexBased,
    worksheetName = None
  )
}

case class ExcelWriteConfig[-A](
  worksheetName: Option[String],
  writeHeaders:  Boolean,
  naming:        String => String,
  styling:       ExcelStyling[A])

object ExcelWriteConfig extends LowPriorityExcelWriteConfig {}

trait LowPriorityExcelWriteConfig {
  implicit def default[A](implicit styling: ExcelStyling[A]): ExcelWriteConfig[A] = ExcelWriteConfig[A](
    worksheetName = None,
    writeHeaders = false,
    naming = Naming.Literal,
    styling = styling
  )
}

sealed trait CellResolutionStrategy {
  def writeHeaders: Boolean

  def getStartOffset(headers: Map[String, Int], name: String, currentOffset: Int): Option[Int]

  def cannotDecodeError(path: List[String], index: Int, cause: String): ExcelDecoderException

  def unableToFindCell(path: List[String], index: Int): ExcelDecoderException
}

object CellResolutionStrategy {
  def pathStr(path: List[String]): String =
    if (path.isEmpty) "root"
    else path.reverse.map(n => s"`$n`").mkString(".")

  final object IndexBased extends CellResolutionStrategy {
    override val writeHeaders: Boolean = false

    override def getStartOffset(headers: Map[String, Int], name: String, currentOffset: Int): Option[Int] =
      Some(currentOffset)

    override def cannotDecodeError(path: List[String], index: Int, cause: String): ExcelDecoderException =
      new ExcelDecoderException(s"Cannot decode cell at index $index: $cause")

    override def unableToFindCell(path: List[String], index: Int): ExcelDecoderException =
      new ExcelDecoderException(s"Unable to find cell at index $index")
  }

  final case class NameBased(naming: Naming = Naming.Literal) extends CellResolutionStrategy {
    override val writeHeaders: Boolean = true

    override def getStartOffset(headers: Map[String, Int], name: String, currentOffset: Int): Option[Int] = {
      val column = naming(name)
      headers.get(column)
    }

    override def cannotDecodeError(path: List[String], index: Int, cause: String): ExcelDecoderException =
      new ExcelDecoderException(
        s"Cannot decode cell at path ${pathStr(path)}: $cause"
      )

    override def unableToFindCell(path: List[String], index: Int): ExcelDecoderException =
      new ExcelDecoderException(s"Unable to find cell at path ${pathStr(path)}")
  }

  trait Custom extends CellResolutionStrategy
}
