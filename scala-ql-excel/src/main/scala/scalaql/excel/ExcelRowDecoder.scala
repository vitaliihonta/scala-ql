package scalaql.excel

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import spire.algebra.DivisionRing
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import scala.annotation.tailrec

case class ReaderContext(
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

case class ReadResult[A](value: A, readCells: Int)

trait ExcelDecoder[A] {
  def read(row: Row)(implicit ctx: ReaderContext): ReadResult[A]
}

trait ExcelSingleCellDecoder[A] extends ExcelDecoder[A] {
  self =>

  def readCell(cell: Cell)(implicit ctx: ReaderContext): A

  override final def read(row: Row)(implicit ctx: ReaderContext): ReadResult[A] = {
    val result = readCell(row.getCell(ctx.startOffset))
    ReadResult(result, readCells = 1)
  }

  def map[B](f: A => B): ExcelDecoder.SingleCell[B] = new ExcelSingleCellDecoder[B] {
    override def readCell(cell: Cell)(implicit ctx: ReaderContext): B = f(self.readCell(cell))
  }
}

object ExcelDecoder extends LowPriorityCellDecoders with ExcelRowDecoderAutoDerivation {
  def apply[A](implicit ev: ExcelDecoder[A]): ev.type = ev

  type SingleCell[A] = ExcelSingleCellDecoder[A]
  def SingleCell[A](implicit ev: ExcelSingleCellDecoder[A]): ev.type = ev
}

class DecoderForCellType[A](cellTypes: Set[CellType])(reader: Cell => A) extends ExcelSingleCellDecoder[A] {
  override def readCell(cell: Cell)(implicit ctx: ReaderContext): A = {
    @tailrec
    def go(input: Cell): A =
      if (cellTypes contains input.getCellType) reader(input)
      else if (input.getCellType == CellType.FORMULA && ctx.evaluateFormulas)
        go(ctx.formulaEvaluator.evaluateInCell(input))
      else {
        val enabledDisabled = if (ctx.evaluateFormulas) "enabled" else "disabled"
        val cellTypeStr = if (cellTypes.size == 1) {
          cellTypes.head.toString
        } else {
          val allTypes = cellTypes.mkString("[", ", ", "]")
          s"one of $allTypes"
        }
        throw ctx.cannotDecodeError(
          s"expected $cellTypeStr cell (evaluate formulas $enabledDisabled), got ${cell.getCellType}"
        )
      }

    go(cell)
  }
}

object DecoderForCellType {
  def apply[A](cellTypes: Set[CellType])(reader: Cell => A): DecoderForCellType[A] =
    new DecoderForCellType[A](cellTypes)(reader)

  def single[A](cellType: CellType)(reader: Cell => A): DecoderForCellType[A] =
    new DecoderForCellType[A](Set(cellType))(reader)
}

trait LowPriorityCellDecoders {

  implicit val stringDecoder: ExcelDecoder.SingleCell[String] =
    DecoderForCellType.single[String](CellType.STRING)(_.getStringCellValue)

  implicit val uuidDecoder: ExcelDecoder.SingleCell[UUID] =
    stringDecoder.map(UUID.fromString)

  implicit val booleanDecoder: ExcelDecoder.SingleCell[Boolean] =
    DecoderForCellType.single[Boolean](CellType.BOOLEAN)(_.getBooleanCellValue)

  class NumericDecoder[N](convertDouble: Double => N, convertString: String => N)
      extends DecoderForCellType[N](Set(CellType.NUMERIC, CellType.STRING))(cell =>
        if (cell.getCellType == CellType.NUMERIC) convertDouble(cell.getNumericCellValue.toLong)
        else convertString(cell.getStringCellValue)
      )

  implicit val intDecoder: ExcelDecoder.SingleCell[Int]       = new NumericDecoder[Int](_.toInt, _.toInt)
  implicit val longDecoder: ExcelDecoder.SingleCell[Long]     = new NumericDecoder[Long](_.toLong, _.toLong)
  implicit val doubleDecoder: ExcelDecoder.SingleCell[Double] = new NumericDecoder[Double](identity, _.toDouble)

  implicit val bigIntDecoder: ExcelDecoder.SingleCell[BigInt] =
    new NumericDecoder[BigInt](d => BigInt(d.toInt), BigInt(_))

  implicit val bigDecimalDecoder: ExcelDecoder.SingleCell[BigDecimal] =
    new NumericDecoder[BigDecimal](BigDecimal(_), BigDecimal(_))

  implicit val localDateTimeDecoder: ExcelDecoder.SingleCell[LocalDateTime] =
    DecoderForCellType.single[LocalDateTime](CellType.NUMERIC)(_.getLocalDateTimeCellValue)

  implicit val localDateDecoder: ExcelDecoder.SingleCell[LocalDate] =
    localDateTimeDecoder.map(_.toLocalDate)
}
