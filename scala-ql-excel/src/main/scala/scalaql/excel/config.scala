package scalaql.excel

import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Cell
import scalaql.sources.Naming
import scalaql.sources.columnar.CodecPath
import java.util.regex.Pattern

case class ExcelReadConfig(
  naming:                 Naming,
  evaluateFormulas:       Boolean,
  choseWorksheet:         Workbook => Sheet,
  cellResolutionStrategy: CellResolutionStrategy)

object ExcelReadConfig {
  val default: ExcelReadConfig = ExcelReadConfig(
    naming = Naming.Literal,
    evaluateFormulas = false,
    choseWorksheet = _.getSheetAt(0),
    cellResolutionStrategy = CellResolutionStrategy.IndexBased
  )
}

case class ExcelWriteConfig[-A](
  worksheetName: Option[String],
  headers:       Boolean,
  naming:        Naming,
  styling:       ExcelStyling[A])

object ExcelWriteConfig {
  def default[A]: ExcelWriteConfig[A] = ExcelWriteConfig[A](
    worksheetName = None,
    headers = false,
    naming = Naming.Literal,
    styling = ExcelStyling.NoStyling
  )
}

sealed trait CellResolutionStrategy {
  def writeHeaders: Boolean

  def getStartOffset(headers: Map[String, Int], location: CodecPath, naming: Naming, currentOffset: Int): Option[Int]
}

object CellResolutionStrategy {

  final case object IndexBased extends CellResolutionStrategy {
    override val writeHeaders: Boolean = false

    override def getStartOffset(
      headers:       Map[String, Int],
      location:      CodecPath,
      naming:        Naming,
      currentOffset: Int
    ): Option[Int] =
      Some(currentOffset)
  }

  final case object NameBased extends CellResolutionStrategy {
    override val writeHeaders: Boolean = true

    override def getStartOffset(
      headers:       Map[String, Int],
      location:      CodecPath,
      naming:        Naming,
      currentOffset: Int
    ): Option[Int] = {
      val column = naming(location.fieldLocation.name)
      headers.get(column)
    }
  }
}
