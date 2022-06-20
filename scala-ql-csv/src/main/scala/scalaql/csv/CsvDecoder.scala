package scalaql.csv

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

case class ReadResult[A](value: A, readCells: Int)

trait CsvDecoder[A] {
  def read(row: Map[String, String])(implicit ctx: CsvContext): ReadResult[A]
}

trait CsvSingleFieldDecoder[A] extends CsvDecoder[A] { self =>
  def readField(value: String)(implicit ctx: CsvContext): A

  override final def read(row: Map[String, String])(implicit ctx: CsvContext): ReadResult[A] = {
    val result = readField(row(ctx.getFieldName))
    ReadResult[A](result, readCells = 1)
  }

  def map[B](f: A => B): CsvDecoder.SingleField[B] = new CsvSingleFieldDecoder[B] {
    override def readField(value: String)(implicit ctx: CsvContext): B =
      f(self.readField(value))
  }
}

object CsvDecoder extends LowPriorityCsvFieldDecoders {
  def apply[A](implicit ev: CsvDecoder[A]): ev.type = ev

  type SingleField[A] = CsvSingleFieldDecoder[A]
  def SingleField[A](implicit ev: CsvDecoder.SingleField[A]): ev.type = ev

  def fieldDecoder[A](f: String => A): CsvDecoder.SingleField[A] =
    new CsvSingleFieldDecoder[A] {
      override def readField(value: String)(implicit ctx: CsvContext): A = f(value)
    }
}

trait LowPriorityCsvFieldDecoders extends CsvDecoderAutoDerivation {
  implicit val stringDecoder: CsvDecoder.SingleField[String]         = CsvDecoder.fieldDecoder(identity[String])
  implicit val booleanDecoder: CsvDecoder.SingleField[Boolean]       = CsvDecoder.fieldDecoder(_.toBoolean)
  implicit val intDecoder: CsvDecoder.SingleField[Int]               = CsvDecoder.fieldDecoder(_.toInt)
  implicit val longDecoder: CsvDecoder.SingleField[Long]             = CsvDecoder.fieldDecoder(_.toLong)
  implicit val doubleDecoder: CsvDecoder.SingleField[Double]         = CsvDecoder.fieldDecoder(_.toDouble)
  implicit val bigIntDecoder: CsvDecoder.SingleField[BigInt]         = CsvDecoder.fieldDecoder(BigInt(_))
  implicit val bigDecimalDecoder: CsvDecoder.SingleField[BigDecimal] = CsvDecoder.fieldDecoder(BigDecimal(_))
  implicit val uuidDecoder: CsvDecoder.SingleField[UUID]             = CsvDecoder.fieldDecoder(UUID.fromString)
  implicit val localDateDecoder: CsvDecoder.SingleField[LocalDate]   = CsvDecoder.fieldDecoder(LocalDate.parse)
  implicit val localDateTimeDecoder: CsvDecoder.SingleField[LocalDateTime] =
    CsvDecoder.fieldDecoder(LocalDateTime.parse)
}
