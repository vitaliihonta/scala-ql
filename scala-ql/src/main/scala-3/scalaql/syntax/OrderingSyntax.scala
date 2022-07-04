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
      f:                 Out => B
    )(implicit ordering: Ordering[B],
      In:                Tag[In],
      Out:               Tag[Out],
      B:                 Tag[B]
    ): Query[In, Out] =
      ${ OrderingSyntax.orderByImpl[In, Out, B]('self, 'f, 'ordering, 'In, 'Out, 'B) }
  }
}

object OrderingSyntax {
  import Scala3MacroUtils.{Call, CallChain}

  def orderByImpl[In: Type, Out: Type, B: Type](
    self:     Expr[Query[In, Out]],
    f:        Expr[Out => B],
    ordering: Expr[Ordering[B]],
    In:       Expr[Tag[In]],
    Out:      Expr[Tag[Out]],
    B:        Expr[Tag[B]]
  )(using q:  Quotes
  ): Expr[Query[In, Out]] = {
    val callChain      = new Scala3MacroUtils[q.type].getCallChain[Out, B](f, Scala3MacroUtils.noop, identity)
    val resultOrdering = getOrdering(callChain, ordering)

    '{
      new Query.OrderByQuery[In, Out, B]($self, $f, Some($B.tag))($In, $Out, $resultOrdering)
    }
  }

  private def getOrdering[A: Type](callChain: CallChain, ordering: Expr[Ordering[A]])(using Quotes): Expr[Ordering[A]] =
    callChain.chain.lastOption match {
      case Some(Call("desc", _)) =>
        '{ $ordering.reverse }
      case other =>
        println(s"Last call is $other")
        ordering
    }
}
