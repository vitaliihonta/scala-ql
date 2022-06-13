package scalaql.visualization

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import scala.annotation.implicitNotFound

@implicitNotFound(
  """
Don't know how to visualize ${A} into table.
Please ensure available implicits for all fields or provide your own instance of ShowAsTable"""
)
trait ShowAsTable[A] {
  def toRow(value: A): Map[String, String]
}

object ShowAsTable extends LowPriorityShowAsTable0 {
  trait Field[A] extends ShowAsTable[A] {
    def toField(value: A): String

    final def toRow(value: A): Map[String, String] = Map(SingleFieldKey -> toField(value))
  }

  def Field[A](implicit ev: ShowAsTable.Field[A]): ev.type = ev

  def apply[A](implicit ev: ShowAsTable[A]): ev.type = ev

  private[scalaql] val SingleFieldKey: String = ""

  def create[A](f: A => Map[String, String]): ShowAsTable[A] =
    new ShowAsTable[A] {
      override def toRow(value: A): Map[String, String] = f(value)
    }

  def field[A](f: A => String): ShowAsTable.Field[A] =
    new ShowAsTable.Field[A] {
      override def toField(value: A): String = f(value)
    }

  def fieldToString[A]: ShowAsTable.Field[A] = field[A](_.toString)
}

trait LowPriorityShowAsTable0 extends LowPriorityShowAsTable1 {

  implicit val showString: ShowAsTable.Field[String]               = ShowAsTable.field(identity[String])
  implicit val showInt: ShowAsTable.Field[Int]                     = ShowAsTable.fieldToString[Int]
  implicit val showDouble: ShowAsTable.Field[Double]               = ShowAsTable.fieldToString[Double]
  implicit val showLong: ShowAsTable.Field[Long]                   = ShowAsTable.fieldToString[Long]
  implicit val showBoolean: ShowAsTable.Field[Boolean]             = ShowAsTable.fieldToString[Boolean]
  implicit val showBigInt: ShowAsTable.Field[BigInt]               = ShowAsTable.fieldToString[BigInt]
  implicit val showBigDecimal: ShowAsTable.Field[BigDecimal]       = ShowAsTable.fieldToString[BigDecimal]
  implicit val showUUID: ShowAsTable.Field[UUID]                   = ShowAsTable.fieldToString[UUID]
  implicit val showLocalDate: ShowAsTable.Field[LocalDate]         = ShowAsTable.fieldToString[LocalDate]
  implicit val showLocalDateTime: ShowAsTable.Field[LocalDateTime] = ShowAsTable.fieldToString[LocalDateTime]

  implicit def showFieldIterable[Col[x] <: Iterable[x], A: ShowAsTable.Field]: ShowAsTable.Field[Col[A]] =
    ShowAsTable.field[Col[A]](_.map(ShowAsTable.Field[A].toField).mkString("[", ", ", "]"))

  implicit def showFieldMap[A: ShowAsTable.Field]: ShowAsTable.Field[Map[String, A]] =
    ShowAsTable.field[Map[String, A]](_.map { case (k, v) =>
      val row = ShowAsTable.Field[A].toField(v)
      s"$k: $row"
    }.mkString("{", ", ", "}"))
}

trait LowPriorityShowAsTable1 extends ShowAsTableDerivation {
  implicit def showIterable[Col[x] <: Iterable[x], A: ShowAsTable]: ShowAsTable.Field[Col[A]] =
    ShowAsTable.field[Col[A]](
      _.map(ShowAsTable[A].toRow(_).map { case (k, v) => s"$k: $v" }.mkString("{", ", ", "}"))
        .mkString("[", ", ", "]")
    )

  implicit def showMap[A: ShowAsTable]: ShowAsTable.Field[Map[String, A]] =
    ShowAsTable.field[Map[String, A]](_.map { case (k, v) =>
      val row = ShowAsTable[A].toRow(v).mkString("{", ", ", "}")
      s"$k: $row"
    }.mkString("{", ", ", "}"))
}
