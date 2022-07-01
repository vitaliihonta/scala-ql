package scalaql.syntax

import izumi.reflect.Tag
import scalaql.*
import spire.algebra.Order

final class SortingSyntax[In, Out](private val self: Query[In, Out]) extends AnyVal {

  def sorted(implicit order: Order[Out], In: Tag[In], Out: Tag[Out]): Query[In, Out] =
    new Query.SortByQuery[In, Out, Out](self, identity, None)

  def sortBy[B: Order: Tag](f: SortBy[Out, B])(implicit In: Tag[In], Out: Tag[Out]): Query[In, Out] =
    new Query.SortByQuery[In, Out, B](self, f, Some(Tag[B].tag))
}
