package scalaql.excel

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import scala.util.control.Exception.catching
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.UUID
import scala.reflect.ClassTag
import scala.annotation.tailrec

case class ReadResult[+A](value: A, readCells: Int)

class ExcelDecoderException(msg: String) extends Exception(msg)
class ExcelDecoderAccumulatingException(name: String, errors: List[ExcelDecoderException])
    extends ExcelDecoderException({
      val errorsStr = errors.map(e => s"( $e )").mkString("\n\t+ ", "\n\t+ ", "\n")
      s"Failed to decode $name:$errorsStr"
    })

trait ExcelDecoder[A] {
  self =>
  def read(row: Row)(implicit ctx: ExcelReadContext): ExcelDecoder.Result[A]

  def map[B](f: A => B): ExcelDecoder[B] = new ExcelDecoder[B] {
    override def read(row: Row)(implicit ctx: ExcelReadContext): ExcelDecoder.Result[B] =
      self.read(row).map(r => ReadResult[B](f(r.value), r.readCells))
  }

  def emap[B](f: A => Either[ExcelDecoderException, B]): ExcelDecoder[B] = new ExcelDecoder[B] {
    override def read(row: Row)(implicit ctx: ExcelReadContext): ExcelDecoder.Result[B] =
      self.read(row).flatMap(r => f(r.value).map(ReadResult[B](_, r.readCells)))
  }
}

trait ExcelSingleCellDecoder[A] extends ExcelDecoder[A] {
  self =>

  def readCell(cell: Cell)(implicit ctx: ExcelReadContext): Either[ExcelDecoderException, A]

  override final def read(row: Row)(implicit ctx: ExcelReadContext): ExcelDecoder.Result[A] = {
    val result = ctx.startOffset.flatMap(idx => readCell(row.getCell(idx)))
    result.map(ReadResult(_, readCells = 1))
  }

  override def map[B](f: A => B): ExcelDecoder.SingleCell[B] = new ExcelSingleCellDecoder[B] {
    override def readCell(cell: Cell)(implicit ctx: ExcelReadContext): Either[ExcelDecoderException, B] =
      self.readCell(cell).map(f)
  }

  override def emap[B](f: A => Either[ExcelDecoderException, B]): ExcelDecoder.SingleCell[B] =
    new ExcelSingleCellDecoder[B] {
      override def readCell(cell: Cell)(implicit ctx: ExcelReadContext): Either[ExcelDecoderException, B] =
        self.readCell(cell).flatMap(f)
    }
}

object ExcelDecoder extends LowPriorityCellDecoders with ExcelRowDecoderAutoDerivation {
  type Result[+A] = Either[ExcelDecoderException, ReadResult[A]]

  def apply[A](implicit ev: ExcelDecoder[A]): ev.type = ev

  type SingleCell[A] = ExcelSingleCellDecoder[A]
  def SingleCell[A](implicit ev: ExcelSingleCellDecoder[A]): ev.type = ev
}

class DecoderForCellType[A](
  cellTypes: Set[CellType]
)(reader:    ExcelReadContext => Cell => Either[ExcelDecoderException, A])
    extends ExcelSingleCellDecoder[A] {
  override def readCell(cell: Cell)(implicit ctx: ExcelReadContext): Either[ExcelDecoderException, A] = {
    @tailrec
    def go(input: Cell): Either[ExcelDecoderException, A] =
      if (cellTypes contains input.getCellType) reader(ctx)(input)
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
        Left(
          ctx.cannotDecodeError(
            s"expected $cellTypeStr cell (evaluate formulas $enabledDisabled), got ${cell.getCellType}"
          )
        )
      }

    go(cell)
  }
}

object DecoderForCellType {
  def apply[A](
    cellTypes: Set[CellType]
  )(reader:    ExcelReadContext => Cell => Either[ExcelDecoderException, A]
  ): DecoderForCellType[A] =
    new DecoderForCellType[A](cellTypes)(reader)

  def single[A](
    cellType: CellType
  )(reader:   ExcelReadContext => Cell => Either[ExcelDecoderException, A]
  ): DecoderForCellType[A] =
    new DecoderForCellType[A](Set(cellType))(reader)

  def singleSafe[A](
    cellType: CellType
  )(reader:   Cell => A
  ): DecoderForCellType[A] =
    new DecoderForCellType[A](Set(cellType))(_ => cell => Right(reader(cell)))

  def catching[E]: CellDecoderCatchPartiallyApplied[E] = new CellDecoderCatchPartiallyApplied[E]()
}

class CellDecoderCatchPartiallyApplied[E] private[excel] (private val `dummy`: Boolean = true) extends AnyVal {
  def apply[A](
    first:        CellType,
    rest:         CellType*
  )(f:            Cell => A
  )(implicit ctg: ClassTag[E]
  ): ExcelDecoder.SingleCell[A] =
    DecoderForCellType[A]((first +: rest).toSet) { implicit ctx => cell =>
      val result = catching(ctg.runtimeClass) either f(cell)
      result.left.map(e => ctx.cannotDecodeError(e.toString))
    }
}

trait LowPriorityCellDecoders {

  implicit val stringDecoder: ExcelDecoder.SingleCell[String] =
    DecoderForCellType.singleSafe[String](CellType.STRING)(_.getStringCellValue)

  implicit val uuidDecoder: ExcelDecoder.SingleCell[UUID] =
    DecoderForCellType.catching[IllegalArgumentException](CellType.STRING)(cell =>
      UUID.fromString(cell.getStringCellValue)
    )

  implicit val booleanDecoder: ExcelDecoder.SingleCell[Boolean] =
    DecoderForCellType.singleSafe[Boolean](CellType.BOOLEAN)(_.getBooleanCellValue)

  def numericDecoder[N](convertDouble: Double => N, convertString: String => N): ExcelDecoder.SingleCell[N] =
    DecoderForCellType.catching[NumberFormatException](CellType.NUMERIC, CellType.STRING) { cell =>
      if (cell.getCellType == CellType.NUMERIC) convertDouble(cell.getNumericCellValue.toLong)
      else convertString(cell.getStringCellValue)
    }

  implicit val intDecoder: ExcelDecoder.SingleCell[Int]       = numericDecoder[Int](_.toInt, _.toInt)
  implicit val longDecoder: ExcelDecoder.SingleCell[Long]     = numericDecoder[Long](_.toLong, _.toLong)
  implicit val doubleDecoder: ExcelDecoder.SingleCell[Double] = numericDecoder[Double](identity, _.toDouble)

  implicit val bigIntDecoder: ExcelDecoder.SingleCell[BigInt] =
    numericDecoder[BigInt](d => BigInt(d.toInt), BigInt(_))

  implicit val bigDecimalDecoder: ExcelDecoder.SingleCell[BigDecimal] =
    numericDecoder[BigDecimal](BigDecimal(_), BigDecimal(_))

  implicit val localDateTimeDecoder: ExcelDecoder.SingleCell[LocalDateTime] =
    DecoderForCellType.single[LocalDateTime](CellType.NUMERIC) { implicit ctx => cell =>
      val result = catching(
        classOf[NumberFormatException],
        classOf[IllegalStateException]
      ).either(cell.getLocalDateTimeCellValue)

      result.left.map(e => ctx.cannotDecodeError(e.toString))
    }

  implicit val localDateDecoder: ExcelDecoder.SingleCell[LocalDate] =
    localDateTimeDecoder.map(_.toLocalDate)
}
