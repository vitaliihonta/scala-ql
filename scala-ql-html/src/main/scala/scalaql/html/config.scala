package scalaql.html

import scalaql.sources.Naming
import scalatags.Text.TypedTag
import scalatags.Text.all.*

case class HtmlTableEncoderConfig[A](
  headTag:  TypedTag[String],
  bodyTag:  TypedTag[String],
  tableTag: TypedTag[String],
  rowTag:   TypedTag[String],
  naming:   Naming,
  styling:  HtmlStyling[A])

object HtmlTableEncoderConfig extends LowPriorityHtmlTableEncoderConfig {}

trait LowPriorityHtmlTableEncoderConfig {
  implicit def default[A](implicit styling: HtmlStyling[A]): HtmlTableEncoderConfig[A] =
    HtmlTableEncoderConfig[A](
      headTag = head(),
      bodyTag = body(),
      tableTag = table(),
      rowTag = tr(),
      naming = Naming.Literal,
      styling = styling
    )
}
