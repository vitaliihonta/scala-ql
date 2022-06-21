package scalaql.html

import language.experimental.macros
import magnolia1.*

trait HtmlTableEncoderAutoDerivation {
  type Typeclass[T] = HtmlTableEncoder[T]

  def join[T](ctx: CaseClass[HtmlTableEncoder, T]): HtmlTableEncoder[T] = new HtmlTableEncoder[T] {

    override val headers: List[String] = ctx.parameters.toList.flatMap { param =>
      val nestedHeaders = param.typeclass.headers
      if (nestedHeaders.isEmpty) List(param.label)
      else nestedHeaders
    }

    override def write(
      value:                 T,
      into:                  Table
    )(implicit writeContext: HtmlTableEncoderContext
    ): Unit =
      ctx.parameters.foreach { param =>
        param.typeclass.write(param.dereference(value), into)(
          writeContext.enterField(param.label)
        )
      }
  }

  implicit def autoDerive[T]: HtmlTableEncoder[T] = macro Magnolia.gen[T]
}
