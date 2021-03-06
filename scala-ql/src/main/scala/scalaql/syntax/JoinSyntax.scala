package scalaql.syntax

import scalaql.Tag
import scalaql.*

class JoinSyntax[In <: From[?], Out](private val self: Query[In, Out]) extends AnyVal {

  /**
   * Entrypoint for building `JOIN` query.
   * */
  def join[In2 <: From[?]: Tag, Out2: Tag](
    that:        Query[In2, Out2]
  )(implicit In: Tag[In],
    Out:         Tag[Out]
  ): Query.InnerJoinPartiallyApplied[In, In2, Out, Out2] =
    new Query.InnerJoinPartiallyApplied[In, In2, Out, Out2](self, that, Query.InnerJoin)

  /**
   * Entrypoint for building `CROSS JOIN` query.
   * */
  def crossJoin[In2 <: From[?]: Tag, Out2: Tag](
    that:        Query[In2, Out2]
  )(implicit In: Tag[In],
    Out:         Tag[Out]
  ): Query.InnerJoinPartiallyApplied[In, In2, Out, Out2] =
    new Query.InnerJoinPartiallyApplied[In, In2, Out, Out2](self, that, Query.CrossJoin)

  /**
   * Entrypoint for building `LEFT JOIN` query.
   * */
  def leftJoin[In2 <: From[?]: Tag, Out2: Tag](
    that:        Query[In2, Out2]
  )(implicit In: Tag[In],
    Out:         Tag[Out]
  ): Query.LeftJoinPartiallyApplied[In, In2, Out, Out2] =
    new Query.LeftJoinPartiallyApplied[In, In2, Out, Out2](self, that)

}
