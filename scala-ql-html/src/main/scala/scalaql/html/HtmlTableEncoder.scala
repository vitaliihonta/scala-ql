package scalaql.html

import scalatags.Text.TypedTag
import scalatags.Text.all.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

case class HtmlTableEncoderContext(
  path:            List[String],
  head:            TypedTag[String],
  nestingStrategy: NestingStrategy,
  bodyStyles:      List[Modifier],
  headerStyles:    String => List[Modifier],
  fieldStyles:     String => List[Modifier]) {

  def currentField: String = path.headOption.getOrElse("$root")

  def getFieldStyles: List[Modifier] = path.headOption.toList.flatMap(headerStyles)
}

object HtmlTableEncoderContext {
  def initial(
    head:            TypedTag[String],
    nestingStrategy: NestingStrategy,
    bodyStyles:      List[Modifier],
    headerStyles:    String => List[Modifier],
    fieldStyles:     String => List[Modifier]
  ): HtmlTableEncoderContext =
    HtmlTableEncoderContext(
      path = Nil,
      head = head,
      nestingStrategy = nestingStrategy,
      bodyStyles = bodyStyles,
      headerStyles = headerStyles,
      fieldStyles = fieldStyles
    )
}

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
  type Result = List[Map[String, TypedTag[String]]]

  def apply[A](implicit ev: HtmlTableEncoder[A]): ev.type = ev

  def columnEncoder[A](f: A => String): HtmlTableEncoder[A] = new HtmlTableEncoder[A] {
    override def headers: List[String] = Nil

    override def write(value: A)(implicit ctx: HtmlTableEncoderContext): HtmlTableEncoder.Result =
      List(Map(ctx.currentField -> td(ctx.getFieldStyles)(f(value))))
  }
}

trait LowPriorityHtmlTableEncoders {
  implicit val stringColumnEncoder: HtmlTableEncoder[String] =
    HtmlTableEncoder.columnEncoder[String](identity)

  def toStringColumnEncoder[A]: HtmlTableEncoder[A] =
    stringColumnEncoder.contramap(_ => Nil)(_.toString)

  implicit val intColumnDecoder: HtmlTableEncoder[Int]                     = toStringColumnEncoder[Int]
  implicit val longColumnDecoder: HtmlTableEncoder[Long]                   = toStringColumnEncoder[Long]
  implicit val doubleColumnDecoder: HtmlTableEncoder[Double]               = toStringColumnEncoder[Double]
  implicit val bigIntColumnDecoder: HtmlTableEncoder[BigInt]               = toStringColumnEncoder[BigInt]
  implicit val bigDecimalColumnDecoder: HtmlTableEncoder[BigDecimal]       = toStringColumnEncoder[BigDecimal]
  implicit val booleanColumnDecoder: HtmlTableEncoder[Boolean]             = toStringColumnEncoder[Boolean]
  implicit val uuidColumnDecoder: HtmlTableEncoder[UUID]                   = toStringColumnEncoder[UUID]
  implicit val localDateColumnDecoder: HtmlTableEncoder[LocalDate]         = toStringColumnEncoder[LocalDate]
  implicit val localDateTimeColumnDecoder: HtmlTableEncoder[LocalDateTime] = toStringColumnEncoder[LocalDateTime]

  implicit def iterableEncoder[Coll[x] <: Iterable[x], A: HtmlTableEncoder]: HtmlTableEncoder[Coll[A]] =
    new HtmlTableEncoder[Coll[A]] {
      override def headers: List[String] = HtmlTableEncoder[A].headers

      override def write(values: Coll[A])(implicit ctx: HtmlTableEncoderContext): HtmlTableEncoder.Result =
        values.toList.flatMap(HtmlTableEncoder[A].write(_))
    }
}
