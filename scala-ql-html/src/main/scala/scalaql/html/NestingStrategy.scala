package scalaql.html

import scalatags.Text.TypedTag

sealed trait NestingStrategy {
  def apply(
    headers: List[String],
    result:  List[HtmlTableEncoder.Result]
  ): HtmlTableEncoder.Result
}

object NestingStrategy {
  object Flatten extends NestingStrategy {
    override def apply(
      headers: List[String],
      result:  List[HtmlTableEncoder.Result]
    ): HtmlTableEncoder.Result =
      result
        .foldLeft(List.empty[Map[String, TypedTag[String]]]) { (acc, next) =>
          next match {
            case List(single) =>
              acc.headOption.map(_ ++ single).map(_ :: acc.tail).getOrElse(List(single))

            case _ =>
              next ::: acc
          }
        }
        .reverse
  }

  // TODO: implement
  class FillGaps(fill: TypedTag[String]) extends NestingStrategy {
    override def apply(
      headers: List[String],
      result:  List[HtmlTableEncoder.Result]
    ): List[Map[String, TypedTag[String]]] = ???
  }
}
