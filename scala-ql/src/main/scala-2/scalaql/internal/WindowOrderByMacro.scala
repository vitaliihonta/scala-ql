package scalaql.internal

import scalaql.*
import scala.reflect.macros.blackbox

class WindowOrderByMacro(override val c: blackbox.Context) extends MacroUtils(c)("window") {
  import c.universe.*

  private val OrderingAny = weakTypeOf[Ordering[Any]]

  def orderBy1[A: WeakTypeTag, B: WeakTypeTag](f: Expr[A => B])(orderingB: Expr[Ordering[B]]): Expr[Window[A]] = {
    val Window  = weakTypeOf[Window[A]]
    val current = getPrefixOf[Window[A]]

    val resultOrderingB = getOrdering(getCallChain(f.tree), orderingB)
    val TagB            = summonTag[B]

    c.Expr[Window[A]](
      q"""
          new $Window(
            $current.__scalaql_window_partitions,
            $current.__scalaql_window_partitionTags,
            ${makeOrder(f, resultOrderingB)} :: $current.__scalaql_window_orders,
            $TagB.tag :: $current.__scalaql_window_orderTags
          )
       """.debugged("Generated window")
    )
  }

  def orderBy2[A: WeakTypeTag, B: WeakTypeTag, C: WeakTypeTag](
    f1:        Expr[A => B],
    f2:        Expr[A => C]
  )(orderingB: Expr[Ordering[B]],
    orderingC: Expr[Ordering[C]]
  ): Expr[Window[A]] = {
    val Window  = weakTypeOf[Window[A]]
    val current = getPrefixOf[Window[A]]

    val resultOrderingB = getOrdering(getCallChain(f1.tree), orderingB)
    val TagB            = summonTag[B]

    val resultOrderingC = getOrdering(getCallChain(f2.tree), orderingC)
    val TagC            = summonTag[C]

    c.Expr[Window[A]](
      q"""
          new $Window(
            $current.__scalaql_window_partitions,
            $current.__scalaql_window_partitionTags,
            ${makeOrder(f2, resultOrderingC)} :: ${makeOrder(f1, resultOrderingB)} :: $current.__scalaql_window_orders,
            $TagC.tag :: $TagB.tag :: $current.__scalaql_window_orderTags
          )
       """.debugged("Generated window")
    )
  }

  def orderBy3[A: WeakTypeTag, B: WeakTypeTag, C: WeakTypeTag, D: WeakTypeTag](
    f1:        Expr[A => B],
    f2:        Expr[A => C],
    f3:        Expr[A => D]
  )(orderingB: Expr[Ordering[B]],
    orderingC: Expr[Ordering[C]],
    orderingD: Expr[Ordering[D]]
  ): Expr[Window[A]] = {
    val Window  = weakTypeOf[Window[A]]
    val current = getPrefixOf[Window[A]]

    val resultOrderingB = getOrdering(getCallChain(f1.tree), orderingB)
    val TagB            = summonTag[B]

    val resultOrderingC = getOrdering(getCallChain(f2.tree), orderingC)
    val TagC            = summonTag[C]

    val resultOrderingD = getOrdering(getCallChain(f3.tree), orderingD)
    val TagD            = summonTag[D]

    c.Expr[Window[A]](
      q"""
          new $Window(
            $current.__scalaql_window_partitions,
            $current.__scalaql_window_partitionTags,
            ${makeOrder(f3, resultOrderingD)} :: ${makeOrder(f2, resultOrderingC)} :: 
              ${makeOrder(f1, resultOrderingB)} :: $current.__scalaql_window_orders,
            $TagD.tag :: $TagC.tag :: $TagB.tag :: $current.__scalaql_window_orderTags
          )
       """.debugged("Generated window")
    )
  }

  private def makeOrder[A, B](f: Expr[A => B], ordering: Expr[Ordering[B]]): Expr[(A => Any, Ordering[Any])] =
    c.Expr[(A => Any, Ordering[Any])](
      q"""
         ($f, $ordering.asInstanceOf[$OrderingAny])
       """
    )
}
