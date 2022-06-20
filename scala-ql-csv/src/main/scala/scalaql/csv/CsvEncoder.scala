package scalaql.csv

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

trait CsvEncoder[A] { self =>
  def headers: List[String]

  def write(value: A)(implicit ctx: CsvContext): CsvEncoder.Result
}

trait CsvSingleFieldEncoder[A] extends CsvEncoder[A] { self =>
  override val headers: List[String] = Nil

  def writeField(value: A)(implicit ctx: CsvContext): String

  override final def write(value: A)(implicit ctx: CsvContext): CsvEncoder.Result = {
    val result = writeField(value)
    Map(ctx.getFieldName -> result)
  }

  def contramap[B](f: B => A): CsvEncoder.SingleField[B] = new CsvEncoder.SingleField[B] {
    override def writeField(value: B)(implicit ctx: CsvContext): String =
      self.writeField(f(value))
  }
}

object CsvEncoder extends LowPriorityCsvFieldEncoders {
  type Result = Map[String, String]

  def apply[A](implicit ev: CsvEncoder[A]): ev.type = ev

  type SingleField[A] = CsvSingleFieldEncoder[A]
  def SingleField[A](implicit ev: CsvEncoder.SingleField[A]): ev.type = ev

  def fieldEncoder[A](f: A => String): CsvEncoder.SingleField[A] = new CsvSingleFieldEncoder[A] {
    override def writeField(value: A)(implicit ctx: CsvContext): String = f(value)
  }
}

trait LowPriorityCsvFieldEncoders extends CsvEncoderAutoDerivation {
  def toStringEncoder[A]: CsvEncoder.SingleField[A] = CsvEncoder.fieldEncoder[A](_.toString)

  implicit val stringEncoder: CsvEncoder.SingleField[String]               = CsvEncoder.fieldEncoder(identity[String])
  implicit val booleanEncoder: CsvEncoder.SingleField[Boolean]             = toStringEncoder[Boolean]
  implicit val intEncoder: CsvEncoder.SingleField[Int]                     = toStringEncoder[Int]
  implicit val longEncoder: CsvEncoder.SingleField[Long]                   = toStringEncoder[Long]
  implicit val doubleEncoder: CsvEncoder.SingleField[Double]               = toStringEncoder[Double]
  implicit val bigIntEncoder: CsvEncoder.SingleField[BigInt]               = toStringEncoder[BigInt]
  implicit val bigDecimalEncoder: CsvEncoder.SingleField[BigDecimal]       = toStringEncoder[BigDecimal]
  implicit val uuidEncoder: CsvEncoder.SingleField[UUID]                   = toStringEncoder[UUID]
  implicit val localDateEncoder: CsvEncoder.SingleField[LocalDate]         = toStringEncoder[LocalDate]
  implicit val localDateTimeEncoder: CsvEncoder.SingleField[LocalDateTime] = toStringEncoder[LocalDateTime]
}
