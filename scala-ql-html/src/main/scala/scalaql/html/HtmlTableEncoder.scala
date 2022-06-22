package scalaql.html

import scalatags.Text.all.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

trait HtmlTableEncoder[A] { self =>
  def headers: List[String]

  def write(value: A, into: HtmlTable)(implicit ctx: HtmlTableEncoderContext): Unit

  def contramap[B](transformHeaders: List[String] => List[String])(f: B => A): HtmlTableEncoder[B] =
    new HtmlTableEncoder[B] {
      override def headers: List[String] = transformHeaders(self.headers)

      override def write(value: B, into: HtmlTable)(implicit ctx: HtmlTableEncoderContext): Unit =
        self.write(f(value), into)
    }
}

object HtmlTableEncoder extends LowPriorityHtmlTableEncoders with HtmlTableEncoderAutoDerivation {

  def apply[A](implicit ev: HtmlTableEncoder[A]): ev.type = ev

  def columnEncoder[A](f: A => String): HtmlTableEncoder[A] = new HtmlTableEncoder[A] {
    override val headers: List[String] = Nil

    override def write(value: A, into: HtmlTable)(implicit ctx: HtmlTableEncoderContext): Unit = {
      val cell = ctx.tdTag(ctx.getFieldStyles)(f(value))
      into.currentRow.append(ctx.fieldLocation.name, cell)
    }
  }
}

trait LowPriorityHtmlTableEncoders {
  implicit val stringColumnEncoder: HtmlTableEncoder[String] =
    HtmlTableEncoder.columnEncoder[String](identity)

  def toStringColumnEncoder[A]: HtmlTableEncoder[A] =
    stringColumnEncoder.contramap(_ => Nil)(_.toString)

  implicit val intColumnDecoder: HtmlTableEncoder[Int]               = toStringColumnEncoder[Int]
  implicit val longColumnDecoder: HtmlTableEncoder[Long]             = toStringColumnEncoder[Long]
  implicit val doubleColumnDecoder: HtmlTableEncoder[Double]         = toStringColumnEncoder[Double]
  implicit val bigIntColumnDecoder: HtmlTableEncoder[BigInt]         = toStringColumnEncoder[BigInt]
  implicit val bigDecimalColumnDecoder: HtmlTableEncoder[BigDecimal] = toStringColumnEncoder[BigDecimal]
  implicit val booleanColumnDecoder: HtmlTableEncoder[Boolean]       = toStringColumnEncoder[Boolean]
  implicit val uuidColumnDecoder: HtmlTableEncoder[UUID]             = toStringColumnEncoder[UUID]
  implicit val localDateColumnDecoder: HtmlTableEncoder[LocalDate]   = toStringColumnEncoder[LocalDate]
  implicit val localDateTimeColumnDecoder: HtmlTableEncoder[LocalDateTime] =
    toStringColumnEncoder[LocalDateTime]

  implicit def optionEncoder[A: HtmlTableEncoder]: HtmlTableEncoder[Option[A]] =
    new HtmlTableEncoder[Option[A]] {
      override val headers: List[String] = HtmlTableEncoder[A].headers

      override def write(values: Option[A], into: HtmlTable)(implicit ctx: HtmlTableEncoderContext): Unit =
        values.foreach { value =>
          HtmlTableEncoder[A].write(value, into)(
            ctx
          )
        }
    }

  implicit def iterableEncoder[Coll[x] <: Iterable[x], A](
    implicit encoder: HtmlTableEncoder[A]
  ): HtmlTableEncoder[Coll[A]] =
    new HtmlTableEncoder[Coll[A]] {
      override val headers: List[String] = HtmlTableEncoder[A].headers

      override def write(values: Coll[A], into: HtmlTable)(implicit ctx: HtmlTableEncoderContext): Unit =
        values.toList.zipWithIndex.foreach { case (value, idx) =>
          HtmlTableEncoder[A].write(value, into.appendEmptyRow)(
            ctx.enterIndex(idx)
          )
        }
    }
}
