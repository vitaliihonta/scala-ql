package scalaql.syntax

import scalaql.*
import scala.quoted.*
import scalaql.utils.Scala3MacroUtils

trait OrderingSyntax {
  extension [A](self: A) {
    def asc: A  = self
    def desc: A = self
  }

  extension [In, Out](self: Query[In, Out]) {
    def ordered(implicit order: Ordering[Out], In: Tag[In], Out: Tag[Out]): Query[In, Out] =
      new Query.OrderByQuery[In, Out, Out](self, identity, None)

    inline def orderBy[B](
      f:                  Out => B
    )(implicit orderingB: Ordering[B],
      In:                 Tag[In],
      Out:                Tag[Out],
      B:                  Tag[B]
    ): Query[In, Out] =
      ${ OrderingSyntax.orderByImpl[In, Out, B]('self, 'f, 'orderingB, 'In, 'Out, 'B) }

    inline def orderBy[B, C](
      f1:                 Out => B,
      f2:                 Out => C
    )(implicit orderingB: Ordering[B],
      orderingC:          Ordering[C],
      In:                 Tag[In],
      Out:                Tag[Out],
      B:                  Tag[B]
    ): Query[In, Out] =
      ${ OrderingSyntax.orderByImpl2[In, Out, B, C]('self, 'f1, 'f2, 'orderingB, 'orderingC, 'In, 'Out, 'B) }
  }
}

object OrderingSyntax {
  import Scala3MacroUtils.*

  def orderByImpl[In: Type, Out: Type, B: Type](
    self:     Expr[Query[In, Out]],
    f:        Expr[Out => B],
    ordering: Expr[Ordering[B]],
    In:       Expr[Tag[In]],
    Out:      Expr[Tag[Out]],
    B:        Expr[Tag[B]]
  )(using q:  Quotes
  ): Expr[Query[In, Out]] = {
    val resultOrdering = getOrdering(accessorCallPath(f, ignoreUnmatched), ordering)

    val By = '{ Tag[B] }

    val makeOrderBy  = f
    val makeOrdering = resultOrdering

    '{
      new Query.OrderByQuery[In, Out, B]($self, $makeOrderBy, Some($By.tag))($In, $Out, $makeOrdering)
    }
  }

  def orderByImpl2[In: Type, Out: Type, B: Type, C: Type](
    self:      Expr[Query[In, Out]],
    f1:        Expr[Out => B],
    f2:        Expr[Out => C],
    orderingB: Expr[Ordering[B]],
    orderingC: Expr[Ordering[C]],
    In:        Expr[Tag[In]],
    Out:       Expr[Tag[Out]],
    B:         Expr[Tag[B]]
  )(using q:   Quotes
  ): Expr[Query[In, Out]] = {
    val resultOrderingB = getOrdering(accessorCallPath(f1, ignoreUnmatched), orderingB)
    val resultOrderingC = getOrdering(accessorCallPath(f2, ignoreUnmatched), orderingC)

    val By           = '{ Tag[(B, C)] }
    val makeOrderBy  = '{ (out: Out) => ($f1(out), $f2(out)) }
    val makeOrdering = '{ Ordering.Tuple2($resultOrderingB, $resultOrderingC) }

    '{
      new Query.OrderByQuery[In, Out, (B, C)]($self, $makeOrderBy, Some($By.tag))($In, $Out, $makeOrdering)
    }
  }

  private def getOrdering[A: Type](
    callChain: List[Call],
    ordering:  Expr[Ordering[A]]
  )(using Quotes
  ): Expr[Ordering[A]] =
    callChain.lastOption match {
      case Some(Call("desc", _)) =>
        '{ $ordering.reverse }
      case other =>
        println(s"Last call is $other")
        ordering
    }
}
