package scalaql.html

import scalatags.Text.all.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

trait HtmlTableEncoder[A] { self =>
  def headers: List[String]

  def write(value: A)(implicit ctx: HtmlTableEncoderContext): HtmlTableEncoder.Result

  def contramap[B](transformHeaders: List[String] => List[String])(f: B => A): HtmlTableEncoder[B] =
    new HtmlTableEncoder[B] {
      override def headers: List[String] = transformHeaders(self.headers)

      override def write(value: B)(implicit ctx: HtmlTableEncoderContext): HtmlTableEncoder.Result =
        self.write(f(value))
    }
}

object HtmlTableEncoder extends LowPriorityHtmlTableEncoders with HtmlTableEncoderAutoDerivation {
  case class Result(value: Modifier, isList: Boolean)

  def apply[A](implicit ev: HtmlTableEncoder[A]): ev.type = ev

  def columnEncoder[A](f: A => String): HtmlTableEncoder[A] = new HtmlTableEncoder[A] {
    override val headers: List[String] = Nil

    override def write(value: A)(implicit ctx: HtmlTableEncoderContext): HtmlTableEncoder.Result =
      Result(td(ctx.getFieldStyles)(f(value)), isList = false)
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

  implicit def iterableEncoder[Coll[x] <: Iterable[x], A](
    implicit encoder: HtmlTableEncoder[A]
  ): HtmlTableEncoder[Coll[A]] =
    new HtmlTableEncoder[Coll[A]] {
      override val headers: List[String] = HtmlTableEncoder[A].headers

      override def write(values: Coll[A])(implicit ctx: HtmlTableEncoderContext): HtmlTableEncoder.Result = {
        val value = values.toList.zipWithIndex.map { case (value, idx) =>
          HtmlTableEncoder[A].write(value)(
            ctx.enterIndex(idx)
          )
        }
        HtmlTableEncoder.Result(value.map(_.value), isList = true)
      }
    }
}
