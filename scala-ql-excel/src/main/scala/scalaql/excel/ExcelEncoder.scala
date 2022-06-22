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

case class WriteResult(cellsWritten: Int)

trait ExcelEncoder[A] {
  def headers: List[String]

  def write(table: ExcelTableApi, value: A)(implicit ctx: ExcelWriteContext): WriteResult
}

trait ExcelSingleCellEncoder[A] extends ExcelEncoder[A] { self =>

  override val headers: List[String] = Nil

  def writeCell(cell: Cell, value: A)(implicit ctx: ExcelWriteContext): Unit

  def write(table: ExcelTableApi, value: A)(implicit ctx: ExcelWriteContext): WriteResult = {
    val row = table.currentRow
    row.insert(
      ctx.startOffset,
      ctx.fieldLocation.name,
      (cell: Cell) => {
        self.writeCell(cell, value)
        ctx.applyCellStyle(cell)
        cell
      }
    )
    WriteResult(cellsWritten = 1)
  }

  def contramap[B](f: B => A): ExcelEncoder.SingleCell[B] = new ExcelSingleCellEncoder[B] {
    override def writeCell(cell: Cell, value: B)(implicit ctx: ExcelWriteContext): Unit =
      self.writeCell(cell, f(value))
  }
}

object ExcelEncoder extends LowPriorityCellEncoders with ExcelRowEncoderAutoDerivation {
  type SingleCell[A] = ExcelSingleCellEncoder[A]

  def apply[A](implicit ev: ExcelEncoder[A]): ev.type = ev
}

trait LowPriorityCellEncoders {

  implicit val stringEncoder: ExcelEncoder.SingleCell[String] =
    new ExcelSingleCellEncoder[String] {
      override def writeCell(cell: Cell, value: String)(implicit ctx: ExcelWriteContext): Unit =
        cell.setCellValue(value)
    }

  implicit val uuidDecoder: ExcelEncoder.SingleCell[UUID] =
    stringEncoder.contramap[UUID](_.toString)

  implicit val booleanEncoder: ExcelEncoder.SingleCell[Boolean] =
    new ExcelSingleCellEncoder[Boolean] {
      override def writeCell(cell: Cell, value: Boolean)(implicit ctx: ExcelWriteContext): Unit =
        cell.setCellValue(value)
    }

  class NumericEncoder[N](toDouble: N => Double) extends ExcelSingleCellEncoder[N] {
    override def writeCell(cell: Cell, value: N)(implicit ctx: ExcelWriteContext): Unit =
      cell.setCellValue(toDouble(value))
  }

  implicit val intEncoder: ExcelEncoder.SingleCell[Int]       = new NumericEncoder[Int](_.toDouble)
  implicit val longEncoder: ExcelEncoder.SingleCell[Long]     = new NumericEncoder[Long](_.toDouble)
  implicit val doubleEncoder: ExcelEncoder.SingleCell[Double] = new NumericEncoder[Double](_.toDouble)

  implicit val bigIntEncoder: ExcelEncoder.SingleCell[BigInt] =
    new ExcelSingleCellEncoder[BigInt] {
      override def writeCell(cell: Cell, value: BigInt)(implicit ctx: ExcelWriteContext): Unit =
        if (value.isValidLong)
          cell.setCellValue(value.toLong)
        else cell.setCellValue(value.toString)
    }

  implicit val bigDecimalEncoder: ExcelEncoder.SingleCell[BigDecimal] =
    new ExcelSingleCellEncoder[BigDecimal] {
      override def writeCell(cell: Cell, value: BigDecimal)(implicit ctx: ExcelWriteContext): Unit =
        if (value.isExactDouble)
          cell.setCellValue(value.toDouble)
        else cell.setCellValue(value.toString)
    }

  implicit val localDateTimeEncoder: ExcelEncoder.SingleCell[LocalDateTime] =
    new ExcelSingleCellEncoder[LocalDateTime] {
      override def writeCell(cell: Cell, value: LocalDateTime)(implicit ctx: ExcelWriteContext): Unit =
        cell.setCellValue(value)
    }

  implicit val localDateEncoder: ExcelEncoder.SingleCell[LocalDate] =
    new ExcelSingleCellEncoder[LocalDate] {
      override def writeCell(cell: Cell, value: LocalDate)(implicit ctx: ExcelWriteContext): Unit =
        cell.setCellValue(value)
    }

  implicit def optionEncoder[A: ExcelEncoder]: ExcelEncoder[Option[A]] =
    new ExcelEncoder[Option[A]] {
      override val headers: List[String] = ExcelEncoder[A].headers

      override def write(table: ExcelTableApi, value: Option[A])(implicit ctx: ExcelWriteContext): WriteResult = {
        val result = value.map { value =>
          ExcelEncoder[A].write(table, value)(
            ctx
          )
        }
        result.getOrElse(WriteResult(cellsWritten = 0))
      }
    }

  implicit def iterableEncoder[Coll[x] <: Iterable[x], A](
    implicit encoder: ExcelEncoder[A]
  ): ExcelEncoder[Coll[A]] =
    new ExcelEncoder[Coll[A]] {
      override val headers: List[String] = ExcelEncoder[A].headers

      override def write(table: ExcelTableApi, values: Coll[A])(implicit ctx: ExcelWriteContext): WriteResult = {
        val results = values.toList.zipWithIndex.map { case (value, idx) =>
          ExcelEncoder[A].write(table.appendEmptyRow, value)(
            ctx.enterIndex(idx)
          )
        }
        results.headOption.getOrElse(WriteResult(cellsWritten = 0))
      }
    }
}
