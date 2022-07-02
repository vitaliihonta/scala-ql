package scalaql.excel

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import scalaql.excel
import scalaql.excel.ExcelDecoder.Result
import scalaql.sources.columnar.CodecPath
import scala.util.control.Exception.catching
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.UUID
import scala.reflect.ClassTag
import scala.annotation.tailrec

case class ReadResult[+A](value: A, readCells: Int)

abstract class ExcelDecoderException(msg: String) extends Exception(msg)
object ExcelDecoderException {
  class CannotDecode(location: CodecPath, rowNumber: Int, cause: String)
      extends ExcelDecoderException(
        s"Cannot decode cell of row number #$rowNumber at path `$location`: $cause"
      )

  class FieldNotFound(location: CodecPath, rowNumber: Int)
      extends ExcelDecoderException(
        s"Field not found for row number #$rowNumber at path `$location`"
      )

  class Accumulating(name: String, val errors: List[ExcelDecoderException])
      extends ExcelDecoderException({
        val errorsStr = errors.map(e => s"( $e )").mkString("\n\t+ ", "\n\t+ ", "\n")
        s"Failed to decode $name:$errorsStr"
      })
}

trait ExcelDecoder[A] extends Serializable {
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

  def supportsBlankOrEmpty: Boolean = false

  def readCell(cell: Cell)(implicit ctx: ExcelReadContext): Either[ExcelDecoderException, A]

  override final def read(row: Row)(implicit ctx: ExcelReadContext): ExcelDecoder.Result[A] =
    ctx.startOffset.flatMap { idx =>
      Option(row.getCell(idx))
        .filter(ableToDecode)
        .toRight(ctx.fieldNotFoundError)
        .flatMap(readCell(_))
        .map(ReadResult(_, readCells = 1))
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

  private def ableToDecode(cell: Cell): Boolean =
    if (cell.getCellType == CellType.BLANK) supportsBlankOrEmpty
    else if (cell.getCellType == CellType.STRING && cell.getStringCellValue.isEmpty) supportsBlankOrEmpty
    else true
}

object ExcelDecoder extends LowPriorityCellDecoders with ExcelRowDecoderAutoDerivation {
  type Result[+A] = Either[ExcelDecoderException, ReadResult[A]]

  def apply[A](implicit ev: ExcelDecoder[A]): ev.type = ev

  type SingleCell[A] = ExcelSingleCellDecoder[A]
  def SingleCell[A](implicit ev: ExcelSingleCellDecoder[A]): ev.type = ev
}

class DecoderForCellType[A](
  cellTypes:        Set[CellType],
  withBlankOrEmpty: Boolean
)(reader:           ExcelReadContext => Cell => Either[ExcelDecoderException, A])
    extends ExcelSingleCellDecoder[A] {

  override val supportsBlankOrEmpty: Boolean = withBlankOrEmpty

  override def readCell(cell: Cell)(implicit ctx: ExcelReadContext): Either[ExcelDecoderException, A] = {
    @tailrec
    def go(input: Cell): Either[ExcelDecoderException, A] =
      if ((cellTypes contains input.getCellType) || supportsBlankOrEmpty) reader(ctx)(input)
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
    cellTypes:        Set[CellType],
    withBlankOrEmpty: Boolean = false
  )(reader:           ExcelReadContext => Cell => Either[ExcelDecoderException, A]
  ): DecoderForCellType[A] =
    new DecoderForCellType[A](cellTypes, withBlankOrEmpty)(reader)

  def single[A](
    cellType:         CellType,
    withBlankOrEmpty: Boolean = false
  )(reader:           ExcelReadContext => Cell => Either[ExcelDecoderException, A]
  ): DecoderForCellType[A] =
    new DecoderForCellType[A](Set(cellType), withBlankOrEmpty)(reader)

  def singleSafe[A](
    cellType:         CellType,
    withBlankOrEmpty: Boolean = false
  )(reader:           Cell => A
  ): DecoderForCellType[A] =
    new DecoderForCellType[A](Set(cellType), withBlankOrEmpty)(_ => cell => Right(reader(cell)))

  def catching[E]: CellDecoderCatchPartiallyApplied[E] = new CellDecoderCatchPartiallyApplied[E]()
}

class CellDecoderCatchPartiallyApplied[E] private[excel] (private val `dummy`: Boolean = true) extends AnyVal {
  def apply[A](
    withBlankOrEmpty: Boolean,
    first:            CellType,
    rest:             CellType*
  )(f:                Cell => A
  )(implicit ctg:     ClassTag[E]
  ): ExcelDecoder.SingleCell[A] =
    DecoderForCellType[A]((first +: rest).toSet, withBlankOrEmpty) { implicit ctx => cell =>
      val result = catching(ctg.runtimeClass) either f(cell)
      result.left.map(e => ctx.cannotDecodeError(e.toString))
    }
}

trait LowPriorityCellDecoders {

  implicit val stringDecoder: ExcelDecoder.SingleCell[String] =
    DecoderForCellType.singleSafe[String](CellType.STRING, withBlankOrEmpty = true)(_.getStringCellValue)

  implicit val uuidDecoder: ExcelDecoder.SingleCell[UUID] =
    DecoderForCellType.catching[IllegalArgumentException](
      withBlankOrEmpty = false,
      CellType.STRING
    )(cell => UUID.fromString(cell.getStringCellValue))

  implicit val booleanDecoder: ExcelDecoder.SingleCell[Boolean] =
    DecoderForCellType.singleSafe[Boolean](CellType.BOOLEAN)(_.getBooleanCellValue)

  def numericDecoder[N](convertDouble: Double => N, convertString: String => N): ExcelDecoder.SingleCell[N] =
    DecoderForCellType.catching[NumberFormatException](
      withBlankOrEmpty = false,
      CellType.NUMERIC,
      CellType.STRING
    ) { cell =>
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
    DecoderForCellType[LocalDateTime](
      Set(
        CellType.NUMERIC,
        CellType.STRING
      )
    ) { implicit ctx => cell =>
      val result =
        if (cell.getCellType == CellType.NUMERIC)
          catching(
            classOf[NumberFormatException],
            classOf[IllegalStateException]
          ).either(cell.getLocalDateTimeCellValue)
        else
          catching(
            classOf[DateTimeParseException],
            classOf[IllegalStateException]
          ).either(LocalDateTime.parse(cell.getStringCellValue))

      result.left.map(e => ctx.cannotDecodeError(e.toString))
    }

  implicit val localDateDecoder: ExcelDecoder.SingleCell[LocalDate] =
    DecoderForCellType[LocalDate](
      Set(
        CellType.NUMERIC,
        CellType.STRING
      )
    ) { implicit ctx => cell =>
      val result =
        if (cell.getCellType == CellType.NUMERIC) {
          catching(
            classOf[NumberFormatException],
            classOf[IllegalStateException]
          ).either(cell.getLocalDateTimeCellValue.toLocalDate)
        } else {
          catching(
            classOf[DateTimeParseException],
            classOf[IllegalStateException]
          ).either(LocalDate.parse(cell.getStringCellValue))
        }

      result.left.map(e => ctx.cannotDecodeError(e.toString))
    }

  implicit def optionDecoder[A: ExcelDecoder]: ExcelDecoder[Option[A]] = new ExcelDecoder[Option[A]] {
    override def read(row: Row)(implicit ctx: ExcelReadContext): ExcelDecoder.Result[Option[A]] =
      ExcelDecoder[A].read(row) match {
        case Left(_: ExcelDecoderException.FieldNotFound) => Right(ReadResult(None, readCells = 0))
        case Left(acc: ExcelDecoderException.Accumulating)
            if acc.errors.forall(_.isInstanceOf[ExcelDecoderException.FieldNotFound]) =>
          Right(ReadResult(None, readCells = 0))
        case other => other.map(rs => ReadResult(Some(rs.value), rs.readCells))
      }
  }
}
