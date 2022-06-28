package scalaql.html

import scalatags.Text.Modifier

trait HtmlStyling[-A] {
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

  implicit lazy val NoStyling: HtmlStyling[Any] = new HtmlStyling[Any] {
    override def headerStyle(name: String): List[Modifier] = Nil

    override def fieldStyle(name: String): List[Modifier] = Nil
  }
}
