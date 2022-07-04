package scalaql.syntax

import scalaql.*
import scalaql.internal.OrderBySyntaxMacro
import scala.language.experimental.macros

final class OrderBySyntax[In, Out](val self: Query[In, Out]) extends AnyVal {

  def ordered(implicit order: Ordering[Out], In: Tag[In], Out: Tag[Out]): Query[In, Out] =
    new Query.OrderByQuery[In, Out, Out](self, identity, None)

  // TODO: add more arity
  def orderBy[B](f: Out => B)(implicit orderingB: Ordering[B]): Query[In, Out] =
    macro OrderBySyntaxMacro.orderBy1[In, Out, B]

  def orderBy[B, C](
    f1:                 Out => B,
    f2:                 Out => C
  )(implicit orderingB: Ordering[B],
    orderingC:          Ordering[C]
  ): Query[In, Out] =
    macro OrderBySyntaxMacro.orderBy2[In, Out, B, C]

  def orderBy[B, C, D](
    f1:                 Out => B,
    f2:                 Out => C,
    f3:                 Out => D
  )(implicit orderingB: Ordering[B],
    orderingC:          Ordering[C],
    orderingD:          Ordering[D]
  ): Query[In, Out] =
    macro OrderBySyntaxMacro.orderBy3[In, Out, B, C, D]
}
