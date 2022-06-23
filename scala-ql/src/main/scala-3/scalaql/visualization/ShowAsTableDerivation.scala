package scalaql.visualization

import scala.deriving.Mirror
import magnolia1.*

trait ShowAsTableDerivation extends ProductDerivation[ShowAsTable] {

  def join[T](ctx: CaseClass[ShowAsTable, T]): ShowAsTable[T] = new ShowAsTable[T] {
    override val headers: List[String] = ctx.params.toList.flatMap { param =>
      val nestedHeaders = param.typeclass.headers
      if (nestedHeaders.isEmpty) List(param.label)
      else nestedHeaders.map(nested => s"${param.label}.$nested")
    }

    override def write(
      value:                 T,
      into:                  ShowTable
    )(implicit writeContext: ShowAsTableContext
    ): Unit =
      ctx.params.foreach { param =>
        param.typeclass.write(param.deref(value), into)(
          writeContext.enterField(param.label)
        )
      }
  }

  inline given autoDerive[T](using Mirror.Of[T]): ShowAsTable[T] = derived[T]
}
