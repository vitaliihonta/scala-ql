package scalaql.internal

import scalaql.*
import scalaql.syntax.OrderBySyntax

import scala.reflect.macros.blackbox

class OrderBySyntaxMacro(override val c: blackbox.Context) extends MacroUtils(c)("orderBy") {
  import c.universe.*

  private val OrderingCompanion = q"_root_.scala.math.Ordering"

  def orderBy1[In: WeakTypeTag, Out: WeakTypeTag, B: WeakTypeTag](
    f:         Expr[Out => B]
  )(orderingB: Expr[Ordering[B]]
  ): Expr[Query[In, Out]] = {
    val OrderByQuery = weakTypeOf[Query.OrderByQuery[In, Out, B]]

    val resultOrderingB = getOrdering(getCallChain(f.tree), orderingB)

    val InTag  = summonTag[In]
    val OutTag = summonTag[Out]
    val query  = getPrefixOf[OrderBySyntax[In, Out]]

    val ByTag        = summonTag[B]
    val makeOrderBy  = f
    val makeOrdering = resultOrderingB

    c.Expr[Query[In, Out]](
      q"""
       new $OrderByQuery($query.self, $makeOrderBy, Some($ByTag.tag))($InTag, $OutTag, $makeOrdering)
       """.debugged("Generated orderBy query")
    )
  }

  def orderBy2[In: WeakTypeTag, Out: WeakTypeTag, B: WeakTypeTag, C: WeakTypeTag](
    f1:        Expr[Out => B],
    f2:        Expr[Out => C]
  )(orderingB: Expr[Ordering[B]],
    orderingC: Expr[Ordering[C]]
  ): Expr[Query[In, Out]] = {
    val OrderByQuery = weakTypeOf[Query.OrderByQuery[In, Out, (B, C)]]

    val resultOrderingB = getOrdering(getCallChain(f1.tree), orderingB)
    val resultOrderingC = getOrdering(getCallChain(f2.tree), orderingC)

    val InTag  = summonTag[In]
    val OutTag = summonTag[Out]
    val query  = getPrefixOf[OrderBySyntax[In, Out]]

    val Out = weakTypeOf[Out]

    val ByTag        = summonTag[(B, C)]
    val makeOrderBy  = q"""(elem: $Out) => ($f1(elem), $f2(elem))"""
    val makeOrdering = q"""$OrderingCompanion.Tuple2($resultOrderingB, $resultOrderingC)"""

    c.Expr[Query[In, Out]](
      q"""
       new $OrderByQuery($query.self, $makeOrderBy, Some($ByTag.tag))($InTag, $OutTag, $makeOrdering)
       """.debugged("Generated orderBy query")
    )
  }

  def orderBy3[In: WeakTypeTag, Out: WeakTypeTag, B: WeakTypeTag, C: WeakTypeTag, D: WeakTypeTag](
    f1:        Expr[Out => B],
    f2:        Expr[Out => C],
    f3:        Expr[Out => D]
  )(orderingB: Expr[Ordering[B]],
    orderingC: Expr[Ordering[C]],
    orderingD: Expr[Ordering[D]]
  ): Expr[Query[In, Out]] = {
    val OrderByQuery = weakTypeOf[Query.OrderByQuery[In, Out, (B, C, D)]]

    val resultOrderingB = getOrdering(getCallChain(f1.tree), orderingB)
    val resultOrderingC = getOrdering(getCallChain(f2.tree), orderingC)
    val resultOrderingD = getOrdering(getCallChain(f3.tree), orderingD)

    val InTag  = summonTag[In]
    val OutTag = summonTag[Out]
    val query  = getPrefixOf[OrderBySyntax[In, Out]]

    val Out = weakTypeOf[Out]

    val ByTag        = summonTag[(B, C, D)]
    val makeOrderBy  = q"""(elem: $Out) => ($f1(elem), $f2(elem), $f3(elem))"""
    val makeOrdering = q"""$OrderingCompanion.Tuple3($resultOrderingB, $resultOrderingC, $resultOrderingD)"""

    c.Expr[Query[In, Out]](
      q"""
       new $OrderByQuery($query.self, $makeOrderBy, Some($ByTag.tag))($InTag, $OutTag, $makeOrdering)
       """.debugged("Generated orderBy query")
    )
  }
}
