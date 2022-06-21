package scalaql.html

import scala.deriving.Mirror
import magnolia1.*

trait HtmlTableEncoderAutoDerivation extends ProductDerivation[HtmlTableEncoder] {

  def join[T](ctx: CaseClass[HtmlTableEncoder, T]): HtmlTableEncoder[T] = new HtmlTableEncoder[T] {

    override val headers: List[String] = ctx.params.toList.flatMap { param =>
      val nestedHeaders = param.typeclass.headers
      if (nestedHeaders.isEmpty) List(param.label)
      else nestedHeaders
    }

    override def write(
      value:                 T,
      into:                  HtmlTable
    )(implicit writeContext: HtmlTableEncoderContext
    ): Unit =
      ctx.params.foreach { param =>
        param.typeclass.write(param.deref(value), into)(
          writeContext.enterField(param.label)
        )
      }
  }

  inline given autoDerive[T](using Mirror.Of[T]): HtmlTableEncoder[T] = derived[T]
}
