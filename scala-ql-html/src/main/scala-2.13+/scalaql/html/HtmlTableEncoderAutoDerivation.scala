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
    ): HtmlTableEncoder.Result =
      writeContext.nestingStrategy(
        headers,
        ctx.parameters.map { param =>
          param.typeclass.write(param.dereference(value))(
            writeContext.copy(path = param.label :: writeContext.path)
          )
        }.toList
      )
  }

  implicit def autoDerive[T]: HtmlTableEncoder[T] = macro Magnolia.gen[T]
}
