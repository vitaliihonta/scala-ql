package scalaql.syntax

import scalaql.*
import scalaql.internal.OrderBySyntaxMacro
import scala.language.experimental.macros

final class OrderBySyntax[In, Out](val self: Query[In, Out]) extends AnyVal {

  def ordered(implicit order: Ordering[Out], In: Tag[In], Out: Tag[Out]): Query[In, Out] =
    new Query.OrderByQuery[In, Out, Out](self, identity, None)

  // TODO: add more arity
  def orderBy[B](f: Out => B)(implicit ordering: Ordering[B]): Query[In, Out] =
    macro OrderBySyntaxMacro.orderBy[In, Out, B]
}
