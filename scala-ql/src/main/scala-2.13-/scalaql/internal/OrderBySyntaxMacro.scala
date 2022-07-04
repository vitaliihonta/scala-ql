package scalaql.internal

import scalaql.*
import scalaql.syntax.OrderBySyntax
import scalaql.utils.MacroUtils
import scala.reflect.macros.blackbox

class OrderBySyntaxMacro(override val c: blackbox.Context) extends MacroUtils(c)("orderBy") {
  import c.universe.*

  def orderBy[In: WeakTypeTag, Out: WeakTypeTag, B: WeakTypeTag](
    f:        Expr[Out => B]
  )(ordering: Expr[Ordering[B]]
  ): Expr[Query[In, Out]] = {
    val OrderByQuery = weakTypeOf[Query.OrderByQuery[In, Out, B]]

    val callChain      = getCallChain(f.tree)
    val resultOrdering = getOrdering(callChain, ordering)

    val In    = summonTag[In]
    val Out   = summonTag[Out]
    val B     = summonTag[B]
    val query = getPrefixOf[OrderBySyntax[In, Out]]

    c.Expr[Query[In, Out]](
      q"""
       new $OrderByQuery($query.self, $f, Some($B.tag))($In, $Out, $resultOrdering)
       """.debugged("Generated orderBy query")
    )
  }
}
