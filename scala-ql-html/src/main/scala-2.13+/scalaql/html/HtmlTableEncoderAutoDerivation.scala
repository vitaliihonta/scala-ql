package scalaql.html

import language.experimental.macros
import magnolia1.*
import scalatags.Text.TypedTag
import scalatags.Text.all.*

trait HtmlTableEncoderAutoDerivation {
  type Typeclass[T] = HtmlTableEncoder[T]

  def join[T](ctx: CaseClass[HtmlTableEncoder, T]): HtmlTableEncoder[T] = new HtmlTableEncoder[T] {

    override val headers: List[String] = ctx.parameters.toList.flatMap { param =>
      val nestedHeaders = param.typeclass.headers
      if (nestedHeaders.isEmpty) List(param.label)
      else nestedHeaders
    }

    override def write(
      value:                 T
    )(implicit writeContext: HtmlTableEncoderContext
    ): HtmlTableEncoder.Result = {
      val encoded = ctx.parameters.map { param =>
        param.label -> param.typeclass.write(param.dereference(value))(
          writeContext.enterField(param.label)
        )
      }.toMap

      val resultValue = tr(fillGapsWithTd(writeContext.headers, encoded))
      HtmlTableEncoder.Result(resultValue, isList = false)
    }

    private def fillGapsWithTd(
      headers: List[String],
      result:  Map[String, HtmlTableEncoder.Result]
    ): List[Modifier] = {
      val forbiddenFilling = result.collect { case (name, HtmlTableEncoder.Result(_, isList @ true)) =>
        name
      }.toSet

      orderedValues(headers) {
        headers.zipWithIndex.flatMap { case (h, offset) =>
          if (!result.contains(h) && !forbiddenFilling(h)) Some((h, offset, td()))
          else None
        } ++ result.zipWithIndex.map { case ((h, v), offset) => (h, offset, v.value) }
      }
    }

    private def orderedValues(headers: List[String])(result: List[(String, Int, Modifier)]): List[Modifier] = {
      val headersOrder = headers.zipWithIndex.toMap
      result
        .sortBy { case (n, offset, _) => headersOrder.getOrElse(n, offset) }
        .map { case (_, _, v) => v }
    }
  }

  implicit def autoDerive[T]: HtmlTableEncoder[T] = macro Magnolia.gen[T]
}
