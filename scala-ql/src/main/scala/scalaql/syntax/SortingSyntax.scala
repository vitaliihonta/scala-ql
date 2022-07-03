package scalaql.syntax

import scalaql.Tag
import scalaql.*

final class SortingSyntax[In, Out](private val self: Query[In, Out]) extends AnyVal {

  def ordered(implicit order: Ordering[Out], In: Tag[In], Out: Tag[Out]): Query[In, Out] =
    new Query.OrderByQuery[In, Out, Out](self, identity, None)

  def orderBy[B: Ordering: Tag](f: OrderBy[Out, B])(implicit In: Tag[In], Out: Tag[Out]): Query[In, Out] =
    new Query.OrderByQuery[In, Out, B](self, f, Some(Tag[B].tag))
}
