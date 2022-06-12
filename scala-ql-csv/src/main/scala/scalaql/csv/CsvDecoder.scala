package scalaql.csv

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

trait CsvDecoder[A] {

  @throws[IllegalArgumentException]
  def read(value: CsvEntry): A

  def map[B](f: A => B): CsvDecoder[B]
}

object CsvDecoder extends LowPriorityCsvFieldDecoders {

  trait Row[A] extends CsvDecoder[A] { self =>
    def readRow(value: CsvEntry.Row): A

    override final def read(value: CsvEntry): A = value match {
      case row: CsvEntry.Row => readRow(row)
      case _                 => throw new IllegalArgumentException("Row decoder expects row, got field")
    }

    final def map[B](f: A => B): CsvDecoder.Row[B] = new CsvDecoder.Row[B] {
      override def readRow(value: CsvEntry.Row): B = f(self.read(value))
    }
  }

  trait Field[A] extends CsvDecoder[A] { self =>
    def readField(value: CsvEntry.Field): A

    override final def read(value: CsvEntry): A = value match {
      case field: CsvEntry.Field => readField(field)
      case _                     => throw new IllegalArgumentException("Field decoder expects field, got row")
    }

    final def map[B](f: A => B): CsvDecoder.Field[B] = new CsvDecoder.Field[B] {
      override def readField(value: CsvEntry.Field): B = f(self.read(value))
    }
  }

  def rowDecoder[A](f: Map[String, String] => A): CsvDecoder.Row[A] = new CsvDecoder.Row[A] {
    override def readRow(value: CsvEntry.Row): A = f(value.row)
  }

  def fieldDecoder[A](f: String => A): CsvDecoder.Field[A] = new CsvDecoder.Field[A] {
    override def readField(value: CsvEntry.Field): A = f(value.field)
  }
}

trait LowPriorityCsvFieldDecoders extends CsvDecoderAutoDerivation {
  implicit val stringDecoder: CsvDecoder.Field[String]               = CsvDecoder.fieldDecoder(identity[String])
  implicit val booleanDecoder: CsvDecoder.Field[Boolean]             = CsvDecoder.fieldDecoder(_.toBoolean)
  implicit val intDecoder: CsvDecoder.Field[Int]                     = CsvDecoder.fieldDecoder(_.toInt)
  implicit val longDecoder: CsvDecoder.Field[Long]                   = CsvDecoder.fieldDecoder(_.toLong)
  implicit val doubleDecoder: CsvDecoder.Field[Double]               = CsvDecoder.fieldDecoder(_.toDouble)
  implicit val bigIntDecoder: CsvDecoder.Field[BigInt]               = CsvDecoder.fieldDecoder(BigInt(_))
  implicit val bigDecimalDecoder: CsvDecoder.Field[BigDecimal]       = CsvDecoder.fieldDecoder(BigDecimal(_))
  implicit val uuidDecoder: CsvDecoder.Field[UUID]                   = CsvDecoder.fieldDecoder(UUID.fromString)
  implicit val localDateDecoder: CsvDecoder.Field[LocalDate]         = CsvDecoder.fieldDecoder(LocalDate.parse)
  implicit val localDateTimeDecoder: CsvDecoder.Field[LocalDateTime] = CsvDecoder.fieldDecoder(LocalDateTime.parse)
}
