package scalaql.html

import scalaql.sources.Naming
import scalatags.Text.TypedTag
import scalatags.Text.all.*

case class HtmlTableEncoderConfig[A](
  htmlTag:  TypedTag[String],
  headTag:  TypedTag[String],
  bodyTag:  TypedTag[String],
  tableTag: TypedTag[String],
  trTag:    TypedTag[String],
  thTag:    TypedTag[String],
  tdTag:    TypedTag[String],
  naming:   Naming,
  styling:  HtmlStyling[A])

object HtmlTableEncoderConfig {
  def default[A]: HtmlTableEncoderConfig[A] =
    HtmlTableEncoderConfig[A](
      htmlTag = html(),
      headTag = head(),
      bodyTag = body(),
      tableTag = table(),
      trTag = tr(),
      thTag = th(),
      tdTag = td(),
      naming = Naming.Literal,
      styling = HtmlStyling.NoStyling
    )
}
