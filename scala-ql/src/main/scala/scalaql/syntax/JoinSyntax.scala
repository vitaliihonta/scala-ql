package scalaql.syntax

import izumi.reflect.Tag
import scalaql.*

class JoinSyntax[In <: From[?], Out](private val self: Query[In, Out]) extends AnyVal {

  def join[In2 <: From[?]: Tag, Out2: Tag](
    that:        Query[In2, Out2]
  )(implicit In: Tag[In],
    Out:         Tag[Out]
  ): Query.InnerJoinPartiallyApplied[In, In2, Out, Out2] =
    new Query.InnerJoinPartiallyApplied[In, In2, Out, Out2](self, that, Query.InnerJoin)

  def crossJoin[In2 <: From[?]: Tag, Out2: Tag](
    that:        Query[In2, Out2]
  )(implicit In: Tag[In],
    Out:         Tag[Out]
  ): Query.InnerJoinPartiallyApplied[In, In2, Out, Out2] =
    new Query.InnerJoinPartiallyApplied[In, In2, Out, Out2](self, that, Query.CrossJoin)

  def leftJoin[In2 <: From[?]: Tag, Out2: Tag](
    that:        Query[In2, Out2]
  )(implicit In: Tag[In],
    Out:         Tag[Out]
  ): Query.LeftJoinPartiallyApplied[In, In2, Out, Out2] =
    new Query.LeftJoinPartiallyApplied[In, In2, Out, Out2](self, that)

}
