package scalaql.csv

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.UUID
import scala.reflect.ClassTag
import scala.util.control.Exception.catching

class CsvDecoderException(msg: String) extends Exception(msg)
class CsvDecoderAccumulatingException(name: String, errors: List[CsvDecoderException])
    extends CsvDecoderException({
      val errorsStr = errors.map(e => s"( $e )").mkString("\n\t+ ", "\n\t+ ", "\n")
      s"Failed to decode $name: $errorsStr"
    })

trait CsvDecoder[A] {
  self =>
  def read(row: Map[String, String])(implicit ctx: CsvContext): CsvDecoder.Result[A]

  def map[B](f: A => B): CsvDecoder[B] = new CsvDecoder[B] {
    override def read(row: Map[String, String])(implicit ctx: CsvContext): CsvDecoder.Result[B] =
      self.read(row).map(f)
  }

  def emap[B](f: A => CsvDecoder.Result[B]): CsvDecoder[B] = new CsvDecoder[B] {
    override def read(row: Map[String, String])(implicit ctx: CsvContext): CsvDecoder.Result[B] =
      self.read(row).flatMap(f)
  }
}

trait CsvSingleFieldDecoder[A] extends CsvDecoder[A] { self =>
  def readField(value: String)(implicit ctx: CsvContext): CsvDecoder.Result[A]

  override final def read(row: Map[String, String])(implicit ctx: CsvContext): CsvDecoder.Result[A] =
    readField(row(ctx.getFieldName))

  override def map[B](f: A => B): CsvDecoder.SingleField[B] = new CsvSingleFieldDecoder[B] {
    override def readField(value: String)(implicit ctx: CsvContext): CsvDecoder.Result[B] =
      self.readField(value).map(f)
  }

  override def emap[B](f: A => CsvDecoder.Result[B]): CsvDecoder.SingleField[B] =
    new CsvSingleFieldDecoder[B] {
      override def readField(value: String)(implicit ctx: CsvContext): CsvDecoder.Result[B] =
        self.readField(value).flatMap(f)
    }
}

object CsvDecoder extends LowPriorityCsvFieldDecoders {
  type Result[+A] = Either[CsvDecoderException, A]

  def apply[A](implicit ev: CsvDecoder[A]): ev.type = ev

  type SingleField[A] = CsvSingleFieldDecoder[A]
  def SingleField[A](implicit ev: CsvDecoder.SingleField[A]): ev.type = ev

  def fieldDecoder[A](f: CsvContext => String => CsvDecoder.Result[A]): CsvDecoder.SingleField[A] =
    new CsvSingleFieldDecoder[A] {
      override def readField(value: String)(implicit ctx: CsvContext): Either[CsvDecoderException, A] = f(ctx)(value)
    }

  def fieldDecodedCatch[E]: FieldDecoderCatchPartiallyApplied[E] =
    new FieldDecoderCatchPartiallyApplied[E]()
}

class FieldDecoderCatchPartiallyApplied[E] private[csv] (private val `dummy`: Boolean = true) extends AnyVal {
  def apply[A](f: String => A)(implicit ctg: ClassTag[E]): CsvDecoder.SingleField[A] =
    CsvDecoder.fieldDecoder[A] { implicit ctx => value =>
      val result = catching(ctg.runtimeClass) either f(value)
      result.left.map(e => ctx.cannotDecodeError(e.toString))
    }
}

trait LowPriorityCsvFieldDecoders extends CsvDecoderAutoDerivation {
  implicit val stringDecoder: CsvDecoder.SingleField[String] = CsvDecoder.fieldDecoder[String](_ => Right(_))

  implicit val booleanDecoder: CsvDecoder.SingleField[Boolean] =
    CsvDecoder.fieldDecodedCatch[IllegalArgumentException](_.toBoolean)

  implicit val intDecoder: CsvDecoder.SingleField[Int] =
    CsvDecoder.fieldDecodedCatch[NumberFormatException](_.toInt)

  implicit val longDecoder: CsvDecoder.SingleField[Long] =
    CsvDecoder.fieldDecodedCatch[NumberFormatException](_.toLong)

  implicit val doubleDecoder: CsvDecoder.SingleField[Double] =
    CsvDecoder.fieldDecodedCatch[NumberFormatException](_.toDouble)

  implicit val bigIntDecoder: CsvDecoder.SingleField[BigInt] =
    CsvDecoder.fieldDecodedCatch[NumberFormatException](BigInt(_))

  implicit val bigDecimalDecoder: CsvDecoder.SingleField[BigDecimal] =
    CsvDecoder.fieldDecodedCatch[NumberFormatException](BigDecimal(_))

  implicit val uuidDecoder: CsvDecoder.SingleField[UUID] =
    CsvDecoder.fieldDecodedCatch[IllegalArgumentException](UUID.fromString)

  implicit val localDateDecoder: CsvDecoder.SingleField[LocalDate] =
    CsvDecoder.fieldDecodedCatch[DateTimeParseException](LocalDate.parse)

  implicit val localDateTimeDecoder: CsvDecoder.SingleField[LocalDateTime] =
    CsvDecoder.fieldDecodedCatch[DateTimeParseException](LocalDateTime.parse)
}
