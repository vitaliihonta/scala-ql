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

trait ExcelEncoder[A] {
  def write(row: Row, value: A)(implicit ctx: ExcelContext): Unit
}

trait ExcelSingleCellEncoder[A] extends ExcelEncoder[A] { self =>

  def writeCell(cell: Cell, value: A)(implicit ctx: ExcelContext): Unit

  def write(row: Row, value: A)(implicit ctx: ExcelContext): Unit = {
    val cell = row.createCell(ctx.startOffset)
    writeCell(cell, value)
  }

  def contramap[B](f: B => A): ExcelEncoder.SingleCell[B] = new ExcelSingleCellEncoder[B] {
    override def writeCell(cell: Cell, value: B)(implicit ctx: ExcelContext): Unit =
      self.writeCell(cell, f(value))
  }
}

object ExcelEncoder extends LowPriorityCellEncoders {
  type SingleCell[A] = ExcelSingleCellEncoder[A]
}

trait LowPriorityCellEncoders {

  implicit val stringEncoder: ExcelEncoder.SingleCell[String] =
    new ExcelSingleCellEncoder[String] {
      override def writeCell(cell: Cell, value: String)(implicit ctx: ExcelContext): Unit =
        cell.setCellValue(value)
    }

  implicit val uuidDecoder: ExcelEncoder.SingleCell[UUID] =
    stringEncoder.contramap[UUID](_.toString)

  implicit val booleanEncoder: ExcelEncoder.SingleCell[Boolean] =
    new ExcelSingleCellEncoder[Boolean] {
      override def writeCell(cell: Cell, value: Boolean)(implicit ctx: ExcelContext): Unit =
        cell.setCellValue(value)
    }

  class NumericEncoder[N](toDouble: N => Double) extends ExcelSingleCellEncoder[N] {
    override def writeCell(cell: Cell, value: N)(implicit ctx: ExcelContext): Unit =
      cell.setCellValue(toDouble(value))
  }

  implicit val intEncoder: ExcelEncoder.SingleCell[Int]       = new NumericEncoder[Int](_.toDouble)
  implicit val longEncoder: ExcelEncoder.SingleCell[Long]     = new NumericEncoder[Long](_.toDouble)
  implicit val doubleEncoder: ExcelEncoder.SingleCell[Double] = new NumericEncoder[Double](_.toDouble)

  implicit val bigIntEncoder: ExcelEncoder.SingleCell[BigInt] =
    new ExcelSingleCellEncoder[BigInt] {
      override def writeCell(cell: Cell, value: BigInt)(implicit ctx: ExcelContext): Unit =
        if (value.isValidLong)
          cell.setCellValue(value.toLong)
        else cell.setCellValue(value.toString)
    }

  implicit val bigDecimalEncoder: ExcelEncoder.SingleCell[BigDecimal] =
    new ExcelSingleCellEncoder[BigDecimal] {
      override def writeCell(cell: Cell, value: BigDecimal)(implicit ctx: ExcelContext): Unit =
        if (value.isExactDouble)
          cell.setCellValue(value.toDouble)
        else cell.setCellValue(value.toString)
    }

  implicit val localDateTimeEncoder: ExcelEncoder.SingleCell[LocalDateTime] =
    new ExcelSingleCellEncoder[LocalDateTime] {
      override def writeCell(cell: Cell, value: LocalDateTime)(implicit ctx: ExcelContext): Unit =
        cell.setCellValue(value)
    }

  implicit val localDateEncoder: ExcelEncoder.SingleCell[LocalDate] =
    new ExcelSingleCellEncoder[LocalDate] {
      override def writeCell(cell: Cell, value: LocalDate)(implicit ctx: ExcelContext): Unit =
        cell.setCellValue(value)
    }
}
