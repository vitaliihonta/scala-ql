package scalaql.html

import scalatags.Text.Modifier

trait HtmlStyling[-A] extends Serializable {
  def headerStyle(name: String): List[Modifier]
  def fieldStyle(name:  String): List[Modifier]
}

object HtmlStyling {
  def apply[A](implicit ev: HtmlStyling[A]): ev.type = ev

  def builder[A]: HtmlStylingBuilder[A] = new HtmlStylingBuilder[A]()

  final class Configured[A](
    header: String => List[Modifier],
    cell:   String => List[Modifier])
      extends HtmlStyling[A] {

    override def headerStyle(name: String): List[Modifier] = header(name)
    override def fieldStyle(name: String): List[Modifier]  = cell(name)
  }

  final case object NoStyling extends HtmlStyling[Any] {
    override def headerStyle(name: String): List[Modifier] = Nil
    override def fieldStyle(name: String): List[Modifier]  = Nil
  }
}
