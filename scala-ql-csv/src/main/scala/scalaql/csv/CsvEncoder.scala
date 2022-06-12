package scalaql.csv

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

trait CsvEncoder[A] { self =>
  def write(value: A): CsvEntry

  def contramap[B](f: B => A): CsvEncoder[B]
}

object CsvEncoder extends LowPriorityCsvFieldEncoders {

  trait Row[A] extends CsvEncoder[A] { self =>
    def write(value: A): CsvEntry.Row

    final def contramap[B](f: B => A): CsvEncoder.Row[B] = new CsvEncoder.Row[B] {
      override def write(value: B): CsvEntry.Row = self.write(f(value))
    }
  }

  trait Field[A] extends CsvEncoder[A] { self =>
    def write(value: A): CsvEntry.Field

    final def contramap[B](f: B => A): CsvEncoder.Field[B] = new CsvEncoder.Field[B] {
      override def write(value: B): CsvEntry.Field = self.write(f(value))
    }
  }

  def rowEncoder[A](f: A => Map[String, String]): CsvEncoder.Row[A] = new CsvEncoder.Row[A] {
    override def write(value: A): CsvEntry.Row = CsvEntry.Row(f(value))
  }

  def fieldEncoder[A](f: A => String): CsvEncoder.Field[A] = new CsvEncoder.Field[A] {
    override def write(value: A): CsvEntry.Field = CsvEntry.Field(f(value))
  }
}

trait LowPriorityCsvFieldEncoders extends CsvEncoderAutoDerivation {
  def toStringEncoder[A]: CsvEncoder.Field[A] = CsvEncoder.fieldEncoder[A](_.toString)

  implicit val stringEncoder: CsvEncoder.Field[String]               = CsvEncoder.fieldEncoder(identity[String])
  implicit val booleanEncoder: CsvEncoder.Field[Boolean]             = toStringEncoder[Boolean]
  implicit val intEncoder: CsvEncoder.Field[Int]                     = toStringEncoder[Int]
  implicit val longEncoder: CsvEncoder.Field[Long]                   = toStringEncoder[Long]
  implicit val doubleEncoder: CsvEncoder.Field[Double]               = toStringEncoder[Double]
  implicit val bigIntEncoder: CsvEncoder.Field[BigInt]               = toStringEncoder[BigInt]
  implicit val bigDecimalEncoder: CsvEncoder.Field[BigDecimal]       = toStringEncoder[BigDecimal]
  implicit val uuidEncoder: CsvEncoder.Field[UUID]                   = toStringEncoder[UUID]
  implicit val localDateEncoder: CsvEncoder.Field[LocalDate]         = toStringEncoder[LocalDate]
  implicit val localDateTimeEncoder: CsvEncoder.Field[LocalDateTime] = toStringEncoder[LocalDateTime]
}
