package scalaql.html

import scalatags.Text.Modifier

trait HtmlStyling[-A] {
  def headerStyle(name: String): List[Modifier]
  def fieldStyle(name:  String): List[Modifier]
}

object HtmlStyling extends LowPriorityHtmlStyling {
  def apply[A](implicit ev: HtmlStyling[A]): ev.type = ev
}

trait LowPriorityHtmlStyling {
  implicit lazy val NoStyling: HtmlStyling[Any] = new HtmlStyling[Any] {
    override def headerStyle(name: String): List[Modifier] = Nil

    override def fieldStyle(name: String): List[Modifier] = Nil
  }
}
