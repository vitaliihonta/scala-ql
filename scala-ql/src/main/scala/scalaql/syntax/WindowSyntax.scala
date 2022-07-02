package scalaql.syntax

import scalaql.*
import scalaql.utils.TupleFlatten

final class WindowSyntax[In, Out](private val self: Query[In, Out]) extends AnyVal {
  def window[B: Tag](
    agg:                   AggregationView[Out] => Aggregation.Of[Out, B]
  )(implicit tupleFlatten: TupleFlatten[(Out, B)],
    In:                    Tag[In]
  ): WindowDsl[In, B, Out, tupleFlatten.Out] =
    new WindowDsl[In, B, Out, tupleFlatten.Out](self, agg, tupleFlatten)
}

final class WindowDsl[In: Tag, B, Out, Res](
  self:         Query[In, Out],
  agg:          AggregationView[Out] => Aggregation.Of[Out, B],
  tupleFlatten: TupleFlatten.Aux[(Out, B), Res]) {

  def over(window: Window[Out]): Query[In, Res] = {
    implicit val outTag: Tag[Res] = tupleFlatten.tag
    new Query.WindowQuery[In, Out, B, Res](
      self,
      agg,
      window,
      tupleFlatten
    )
  }

  def over(f: Window[Out] => Window[Out]): Query[In, Res] =
    over(f(Window[Out]))
}
