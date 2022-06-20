package scalaql.html

import scalatags.Text.TypedTag
import scalatags.Text.all.*

class HtmlRenderer[A](encoder: HtmlTableEncoder[A]) {
  def render(value: A): TypedTag[String] = ???
}

//object HtmlRenderer {
//  implicit
//}