package scalaql.syntax

import scalaql.*
import scalaql.utils.TupleFlatten

final class WindowSyntax[In, Out](private val self: Query[In, Out]) extends AnyVal {
  def window[Res](
    agg:         QueryExpressionBuilder[Out] => QueryExpression.Of[Out, Res]
  )(implicit In: Tag[In],
    flatten:     TupleFlatten[(Out, Res)]
  ): WindowDsl[In, Out, Res, flatten.Out] =
    new WindowDsl[In, Out, Res, flatten.Out](self, agg)(In, flatten)
}

final class WindowDsl[In: Tag, Out, Res, B](
  self:             Query[In, Out],
  agg:              QueryExpressionBuilder[Out] => QueryExpression.Of[Out, Res]
)(implicit flatten: TupleFlatten.Of[(Out, Res), B]) {

  def over(window: Window[Out])(implicit B: Tag[B]): Query[In, B] =
    new Query.WindowQuery[In, Out, Res, B](
      self,
      agg,
      window,
      flatten
    )

  def over(f: Window.Builder[Out] => Window[Out])(implicit B: Tag[B]): Query[In, B] =
    over(f(Window[Out]))
}
