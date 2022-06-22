package scalaql.csv

import scalaql.sources.columnar.CodecPath
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.UUID
import scala.reflect.ClassTag
import scala.util.control.Exception.catching

abstract class CsvDecoderException(msg: String) extends Exception(msg)
object CsvDecoderException {
  class CannotDecode(location: CodecPath, cause: String)
      extends CsvDecoderException(
        s"Cannot decode cell at path `$location`: $cause"
      )

  class FieldNotFound(location: CodecPath)
      extends CsvDecoderException(
        s"Field not found at path `$location`"
      )

  class Accumulating(name: String, val errors: List[CsvDecoderException])
      extends CsvDecoderException({
        val errorsStr = errors.map(e => s"( $e )").mkString("\n\t+ ", "\n\t+ ", "\n")
        s"Failed to decode $name:$errorsStr"
      })
}

trait CsvDecoder[A] {
  self =>
  def read(row: Map[String, String])(implicit ctx: CsvReadContext): CsvDecoder.Result[A]

  def map[B](f: A => B): CsvDecoder[B] = new CsvDecoder[B] {
    override def read(row: Map[String, String])(implicit ctx: CsvReadContext): CsvDecoder.Result[B] =
      self.read(row).map(f)
  }

  def emap[B](f: A => CsvDecoder.Result[B]): CsvDecoder[B] = new CsvDecoder[B] {
    override def read(row: Map[String, String])(implicit ctx: CsvReadContext): CsvDecoder.Result[B] =
      self.read(row).flatMap(f)
  }
}

trait CsvSingleFieldDecoder[A] extends CsvDecoder[A] { self =>
  def supportsEmptyCell: Boolean = false

  def readField(value: String)(implicit ctx: CsvReadContext): CsvDecoder.Result[A]

  override final def read(row: Map[String, String])(implicit ctx: CsvReadContext): CsvDecoder.Result[A] =
    row
      .get(ctx.getFieldName)
      .toRight(ctx.fieldNotFoundError)
      .filterOrElse(_.nonEmpty || supportsEmptyCell, ctx.fieldNotFoundError)
      .flatMap(readField(_))

  override def map[B](f: A => B): CsvDecoder.SingleField[B] = new CsvSingleFieldDecoder[B] {
    override def readField(value: String)(implicit ctx: CsvReadContext): CsvDecoder.Result[B] =
      self.readField(value).map(f)
  }

  override def emap[B](f: A => CsvDecoder.Result[B]): CsvDecoder.SingleField[B] =
    new CsvSingleFieldDecoder[B] {
      override def readField(value: String)(implicit ctx: CsvReadContext): CsvDecoder.Result[B] =
        self.readField(value).flatMap(f)
    }
}

object CsvDecoder extends LowPriorityCsvFieldDecoders {
  type Result[+A] = Either[CsvDecoderException, A]

  def apply[A](implicit ev: CsvDecoder[A]): ev.type = ev

  type SingleField[A] = CsvSingleFieldDecoder[A]
  def SingleField[A](implicit ev: CsvDecoder.SingleField[A]): ev.type = ev

  def fieldDecoder[A](
    withEmptyCells: Boolean = false
  )(f:              CsvReadContext => String => CsvDecoder.Result[A]
  ): CsvDecoder.SingleField[A] =
    new CsvSingleFieldDecoder[A] {
      override val supportsEmptyCell: Boolean = withEmptyCells
      override def readField(value: String)(implicit ctx: CsvReadContext): Either[CsvDecoderException, A] =
        f(ctx)(value)
    }

  def fieldDecodedCatch[E](withEmptyCells: Boolean = false): FieldDecoderCatchPartiallyApplied[E] =
    new FieldDecoderCatchPartiallyApplied[E](withEmptyCells)
}

class FieldDecoderCatchPartiallyApplied[E] private[csv] (private val withEmptyCells: Boolean) extends AnyVal {
  def apply[A](f: String => A)(implicit ctg: ClassTag[E]): CsvDecoder.SingleField[A] =
    CsvDecoder.fieldDecoder[A](withEmptyCells) { implicit ctx => value =>
      val result = catching(ctg.runtimeClass) either f(value)
      result.left.map(e => ctx.cannotDecodeError(e.toString))
    }
}

trait LowPriorityCsvFieldDecoders extends CsvDecoderAutoDerivation {
  implicit val stringDecoder: CsvDecoder.SingleField[String] =
    CsvDecoder.fieldDecoder[String](withEmptyCells = true)(_ => Right(_))

  implicit val booleanDecoder: CsvDecoder.SingleField[Boolean] =
    CsvDecoder.fieldDecodedCatch[IllegalArgumentException](withEmptyCells = false)(_.toBoolean)

  implicit val intDecoder: CsvDecoder.SingleField[Int] =
    CsvDecoder.fieldDecodedCatch[NumberFormatException](withEmptyCells = false)(_.toInt)

  implicit val longDecoder: CsvDecoder.SingleField[Long] =
    CsvDecoder.fieldDecodedCatch[NumberFormatException](withEmptyCells = false)(_.toLong)

  implicit val doubleDecoder: CsvDecoder.SingleField[Double] =
    CsvDecoder.fieldDecodedCatch[NumberFormatException](withEmptyCells = false)(_.toDouble)

  implicit val bigIntDecoder: CsvDecoder.SingleField[BigInt] =
    CsvDecoder.fieldDecodedCatch[NumberFormatException](withEmptyCells = false)(BigInt(_))

  implicit val bigDecimalDecoder: CsvDecoder.SingleField[BigDecimal] =
    CsvDecoder.fieldDecodedCatch[NumberFormatException](withEmptyCells = false)(BigDecimal(_))

  implicit val uuidDecoder: CsvDecoder.SingleField[UUID] =
    CsvDecoder.fieldDecodedCatch[IllegalArgumentException](withEmptyCells = false)(UUID.fromString)

  implicit val localDateDecoder: CsvDecoder.SingleField[LocalDate] =
    CsvDecoder.fieldDecodedCatch[DateTimeParseException](withEmptyCells = false)(LocalDate.parse)

  implicit val localDateTimeDecoder: CsvDecoder.SingleField[LocalDateTime] =
    CsvDecoder.fieldDecodedCatch[DateTimeParseException](withEmptyCells = false)(LocalDateTime.parse)

  implicit def optionDecoder[A: CsvDecoder]: CsvDecoder[Option[A]] =
    new CsvDecoder[Option[A]] {
      override def read(row: Map[String, String])(implicit ctx: CsvReadContext): CsvDecoder.Result[Option[A]] =
        CsvDecoder[A].read(row) match {
          case Left(_: CsvDecoderException.FieldNotFound) => Right(None)
          case Left(acc: CsvDecoderException.Accumulating)
              if acc.errors.forall(_.isInstanceOf[CsvDecoderException.FieldNotFound]) =>
            Right(None)
          case other => other.map(Some(_))
        }
    }
}
