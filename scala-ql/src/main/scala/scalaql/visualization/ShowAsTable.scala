package scalaql.visualization

import scalaql.sources.columnar.{CodecPath, TableApiWriteContext}
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import scala.annotation.implicitNotFound

case class ShowAsTableContext(
  location: CodecPath,
  headers:  List[String])
    extends TableApiWriteContext[ShowAsTableContext] { self =>

  override def enterField(name: String): ShowAsTableContext =
    copy(location = CodecPath.AtField(name, self.location))

  override def enterIndex(idx: Int): ShowAsTableContext =
    copy(location = CodecPath.AtIndex(idx, self.location.fieldLocation))
}

object ShowAsTableContext {
  def initial(headers: List[String]): ShowAsTableContext =
    ShowAsTableContext(CodecPath.Root, headers)
}

@implicitNotFound(
  """
Don't know how to visualize ${A} into table.
Please ensure available implicits for all fields or provide your own instance of ShowAsTable"""
)
trait ShowAsTable[A] {
  def headers: List[String]

  def write(value: A, into: ShowTable)(implicit ctx: ShowAsTableContext): Unit
}

object ShowAsTable extends ShowAsTableDerivation with LowPriorityShowAsTable0 {
  trait Field[A] extends ShowAsTable[A] {
    override val headers: List[String] = Nil

    def writeField(value: A): String

    override def write(value: A, into: ShowTable)(implicit ctx: ShowAsTableContext): Unit =
      into.currentRow.append(ctx.fieldLocation.name, writeField(value))
  }

  def Field[A](implicit ev: ShowAsTable.Field[A]): ev.type = ev

  def apply[A](implicit ev: ShowAsTable[A]): ev.type = ev

  def field[A](f: A => String): ShowAsTable.Field[A] =
    new ShowAsTable.Field[A] {
      override def writeField(value: A): String = f(value)
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

  implicit def showFieldOption[A: ShowAsTable.Field]: ShowAsTable.Field[Option[A]] =
    ShowAsTable.field[Option[A]](_.map(ShowAsTable.Field[A].writeField).getOrElse("null"))

  implicit def showFieldIterable[Col[x] <: Iterable[x], A: ShowAsTable.Field]: ShowAsTable.Field[Col[A]] =
    ShowAsTable.field[Col[A]](_.map(ShowAsTable.Field[A].writeField).mkString("[", ", ", "]"))

  implicit def showFieldMap[A: ShowAsTable.Field]: ShowAsTable.Field[Map[String, A]] =
    ShowAsTable.field[Map[String, A]](_.map { case (k, v) =>
      val row = ShowAsTable.Field[A].writeField(v)
      s"$k: $row"
    }.mkString("{", ", ", "}"))
}

trait LowPriorityShowAsTable1 {
  implicit def showIterable[Col[x] <: Iterable[x], A: ShowAsTable]: ShowAsTable[Col[A]] =
    new ShowAsTable[Col[A]] {
      override def headers: List[String] = Nil

      override def write(values: Col[A], into: ShowTable)(implicit ctx: ShowAsTableContext): Unit = {
        val innerTable = ShowTable.empty
        values.zipWithIndex.foreach { case (value, idx) =>
          ShowAsTable[A].write(value, innerTable.appendEmptyRow)(
            ctx.enterIndex(idx)
          )
        }

        val result = innerTable.getRowValues
          .map(row => row.map { case (k, v) => s"$k: $v" }.mkString("{", ", ", "}"))
          .mkString("[", ", ", "]")

        into.currentRow.append(ctx.fieldLocation.name, result)
      }
    }

  implicit def showMap[A: ShowAsTable]: ShowAsTable[Map[String, A]] =
    new ShowAsTable[Map[String, A]] {
      override def headers: List[String] = Nil

      override def write(values: Map[String, A], into: ShowTable)(implicit ctx: ShowAsTableContext): Unit = {
        val innerTable = ShowTable.empty
        values.foreach { case (k, value) =>
          ShowAsTable[A].write(value, innerTable.appendEmptyRow)(
            ctx.enterField(k)
          )
        }

        val result = innerTable.getRowValues
          .map(row => row.map { case (k, v) => s"$k: $v" }.mkString("{", ", ", "}"))
          .mkString("[", ", ", "]")

        into.currentRow.append(ctx.fieldLocation.name, result)
      }
    }

  implicit def showOption[A: ShowAsTable]: ShowAsTable[Option[A]] =
    new ShowAsTable[Option[A]] {
      override def headers: List[String] = ShowAsTable[A].headers

      override def write(value: Option[A], into: ShowTable)(implicit ctx: ShowAsTableContext): Unit =
        value.foreach(ShowAsTable[A].write(_, into))
    }
}
