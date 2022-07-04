package scalaql.internal

import scalaql.*
import scalaql.Window
import scalaql.utils.MacroUtils
import scala.reflect.macros.blackbox

class WindowOrderByMacro(override val c: blackbox.Context) extends MacroUtils(c)("window") {
  import c.universe.*

  private val OrderingAny = weakTypeOf[Ordering[Any]]

  def orderBy[A: WeakTypeTag, B: WeakTypeTag](f: Expr[A => B])(ordering: Expr[Ordering[B]]): Expr[Window[A]] = {
    val Window = weakTypeOf[Window[A]]

    val callChain      = getCallChain(f.tree)
    val resultOrdering = getOrdering(callChain, ordering)

    val TagB    = summonTag[B]
    val current = getPrefixOf[Window[A]]

    c.Expr[Window[A]](
      q"""
          new $Window(
            $current.__scalaql_window_partitions,
            $current.__scalaql_window_partitionTags,
            ${makeOrder(f, resultOrdering)} :: $current.__scalaql_window_orders,
            $TagB.tag :: $current.__scalaql_window_orderTags
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
