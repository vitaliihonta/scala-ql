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

sealed trait ExcelInput
object ExcelInput {
  case class CellInput(cell: Cell) extends ExcelInput
  case class RowInput(row: Row)    extends ExcelInput
}

case class ReaderContext(workbook: Workbook, evaluateFormulas: Boolean) {
  lazy val formulaEvaluator: FormulaEvaluator = workbook.getCreationHelper.createFormulaEvaluator
}

trait ExcelDecoder[A] {
  def read(input: ExcelInput)(implicit ctx: ReaderContext): A
}

trait ExcelCellDecoder[A] extends ExcelDecoder[A] { self =>
  def readCell(cell: Cell)(implicit ctx: ReaderContext): A

  def map[B](f: A => B): ExcelCellDecoder[B] = new ExcelCellDecoder[B] {
    override def readCell(cell: Cell)(implicit ctx: ReaderContext): B = f(self.readCell(cell))
  }

  override final def read(input: ExcelInput)(implicit ctx: ReaderContext): A = input match {
    case ExcelInput.CellInput(cell) => readCell(cell)
    case _                          => throw new IllegalArgumentException("ExcelCellDecoder expects a cell, got a row")
  }
}

trait ExcelRowDecoder[A] extends ExcelDecoder[A] {
  def readRow(row: Row)(implicit ctx: ReaderContext): A

  override final def read(input: ExcelInput)(implicit ctx: ReaderContext): A = input match {
    case ExcelInput.RowInput(row) => readRow(row)
    case _                        => throw new IllegalArgumentException("ExcelRowDecoder expects a row, got a cell")
  }
}

object ExcelDecoder extends LowPriorityCellDecoders {
  type Row[A] = ExcelRowDecoder[A]
  def Row[A](implicit ev: ExcelRowDecoder[A]): ev.type = ev

  type Cell[A] = ExcelCellDecoder[A]
  def Cell[A](implicit ev: ExcelCellDecoder[A]): ev.type = ev
}

class DecoderForCellType[A](cellTypes: Set[CellType])(reader: Cell => A) extends ExcelCellDecoder[A] {
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
        throw new IllegalArgumentException(
          s"Expected $cellTypeStr (evaulate formulas $enabledDisabled) cell, got ${cell.getCellType}"
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

  implicit val stringDecoder: ExcelDecoder.Cell[String] =
    DecoderForCellType.single[String](CellType.STRING)(_.getStringCellValue)

  implicit val uuidDecoder: ExcelDecoder.Cell[UUID] =
    stringDecoder.map(UUID.fromString)

  implicit val booleanDecoder: ExcelDecoder.Cell[Boolean] =
    DecoderForCellType.single[Boolean](CellType.BOOLEAN)(_.getBooleanCellValue)

  class NumericDecoder[N](convert: Double => N)
      extends DecoderForCellType[N](Set(CellType.NUMERIC))(c => convert(c.getNumericCellValue))

  implicit val intDecoder: ExcelDecoder.Cell[Int]       = new NumericDecoder[Int](_.toInt)
  implicit val longDecoder: ExcelDecoder.Cell[Long]     = new NumericDecoder[Long](_.toLong)
  implicit val doubleDecoder: ExcelDecoder.Cell[Double] = new NumericDecoder[Double](identity)

  implicit val localDateTimeDecoder: ExcelDecoder.Cell[LocalDateTime] =
    DecoderForCellType.single[LocalDateTime](CellType.NUMERIC)(_.getLocalDateTimeCellValue)

  implicit val localDateDecoder: ExcelDecoder.Cell[LocalDate] =
    localDateTimeDecoder.map(_.toLocalDate)

  implicit val bigIntDecoder: ExcelDecoder.Cell[BigInt] =
    DecoderForCellType[BigInt](Set(CellType.NUMERIC, CellType.STRING)) { cell =>
      if (cell.getCellType == CellType.NUMERIC) BigInt(cell.getNumericCellValue.toLong)
      else BigInt(cell.getStringCellValue)
    }

  implicit val bigDecimalDecoder: ExcelDecoder.Cell[BigDecimal] =
    DecoderForCellType[BigDecimal](Set(CellType.NUMERIC, CellType.STRING)) { cell =>
      if (cell.getCellType == CellType.NUMERIC) BigDecimal(cell.getNumericCellValue)
      else BigDecimal(cell.getStringCellValue)
    }
}
