package scalaql.excel

import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Cell
import java.util.regex.Pattern

case class ExcelConfig(
  evaluateFormulas:       Boolean,
  choseWorksheet:         Workbook => Sheet,
  cellResolutionStrategy: CellResolutionStrategy)

object ExcelConfig {
  implicit val default: ExcelConfig = ExcelConfig(
    evaluateFormulas = false,
    choseWorksheet = _.getSheetAt(0),
    cellResolutionStrategy = CellResolutionStrategy.IndexBased
  )
}

sealed trait CellResolutionStrategy {
  def getStartOffset(headers: Map[String, Int], name: String, currentOffset: Int): Int
}

object CellResolutionStrategy {
  final object IndexBased extends CellResolutionStrategy {
    override def getStartOffset(headers: Map[String, Int], name: String, currentOffset: Int): Int =
      currentOffset
  }

  final case class NameBased(naming: String => String = Naming.Literal) extends CellResolutionStrategy {
    override def getStartOffset(headers: Map[String, Int], name: String, currentOffset: Int): Int = {
      val column = naming(name)
      headers(column)
    }
  }

  trait Custom extends CellResolutionStrategy
}

object Naming {

  private val basePattern: Pattern = Pattern.compile("([A-Z]+)([A-Z][a-z])")
  private val swapPattern: Pattern = Pattern.compile("([a-z\\d])([A-Z])")

  val Literal: String => String = identity[String]

  val SnakeCase: String => String = s => {
    val partial = basePattern.matcher(s).replaceAll("$1_$2")
    swapPattern.matcher(partial).replaceAll("$1_$2").toLowerCase
  }

  val ScreamingSnakeCase: String => String = s => {
    val partial = basePattern.matcher(s).replaceAll("$1_$2")
    swapPattern.matcher(partial).replaceAll("$1_$2").toUpperCase
  }

  val KebabCase: String => String = s => {
    val partial = basePattern.matcher(s).replaceAll("$1-$2")
    swapPattern.matcher(partial).replaceAll("$1-$2").toLowerCase
  }

  val WithSpaces: String => String = s => {
    val partial = basePattern.matcher(s).replaceAll("$1 $2")
    swapPattern.matcher(partial).replaceAll("$1 $2").toLowerCase
  }
}
