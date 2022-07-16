package scalaql.syntax

import scalaql.*
import scalaql.utils.TupleFlatten

final class WindowSyntax[In, Out](private val self: Query[In, Out]) extends AnyVal {

  /**
   * Entrypoint for building a windowed query - equivalent of plain SQL window functions.
   *
   * Example:
   * {{{
   *   select[Order]
   *   .window(
   *     _.avgBy(_.unitPrice)
   *   )
   *   .over(
   *     _.partitionBy(_.customerId)
   *      .orderBy(_.orderDate.desc)
   *   )
   * }}}
   *
   * @see [[https://www.geeksforgeeks.org/window-functions-in-sql/ SQL window functions]]
   *
   * @tparam Res window function result
   * @param agg window function aggregation or ranking
   * @return an entrypoint for defining window functions
   * */
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

  /**
   * Applies the given aggregation/ranking over the specified window.
   *
   * @param f the window with partitionBy/orderBy
   * @return a query producing results of executing this window function.
   * */
  def over(f: WindowBuilder[Out] => Window[Out])(implicit B: Tag[B]): Query[In, B] =
    over(f(Window[Out]))

  /**
   * Applies the given aggregation/ranking over the specified window.
   *
   * @note this method should only be used in case you're reusing common window spec.
   * 
   * @param window the window with partitionBy/orderBy
   * @return a query producing results of executing this window function.
   * */
  def over(window: Window[Out])(implicit B: Tag[B]): Query[In, B] =
    new Query.WindowQuery[In, Out, Res, B](
      self,
      agg,
      window,
      flatten
    )
}
