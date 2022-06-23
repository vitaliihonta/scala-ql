package scalaql.visualization

import language.experimental.macros
import magnolia1.*

trait ShowAsTableDerivation {
  type Typeclass[T] = ShowAsTable[T]

  def join[T](ctx: CaseClass[ShowAsTable, T]): ShowAsTable[T] = new ShowAsTable[T] {
    override val headers: List[String] = ctx.parameters.toList.flatMap { param =>
      val nestedHeaders = param.typeclass.headers
      if (nestedHeaders.isEmpty) List(param.label)
      else nestedHeaders.map(nested => s"${param.label}.$nested")
    }

    override def write(
      value:                 T,
      into:                  ShowTable
    )(implicit writeContext: ShowAsTableContext
    ): Unit =
      ctx.parameters.foreach { param =>
        param.typeclass.write(param.dereference(value), into)(
          writeContext.enterField(param.label)
        )
      }
  }

  implicit def autoDerive[T]: ShowAsTable[T] = macro Magnolia.gen[T]
}
