package scalaql.syntax

import scalaql.*
import scalaql.internal.GroupBySyntaxMacro

import scala.language.experimental.macros

final class GroupBySyntax[In, Out](@internalApi val self: Query[In, Out]) extends AnyVal {

  // TODO: Implement for scala3
  def groupBy[A](f: Out => A): GroupedQuery1[In, Out, A] =
    macro GroupBySyntaxMacro.groupBy1Impl[In, Out, A]

  /**
   * Entrypoint for performing aggregations on this query.
   *
   * @tparam A the first grouping key type
   * @tparam B the second grouping key type
   * @param f1 extracts the first grouping key
   * @param f2 extracts the second grouping key
   * */
  def groupBy[A, B](
    f1: Out => A,
    f2: Out => B
  ): GroupedQuery2[In, Out, A, B] =
    macro GroupBySyntaxMacro.groupBy2Impl[In, Out, A, B]

  /**
   * Entrypoint for performing aggregations on this query.
   *
   * @tparam A the first grouping key type
   * @tparam B the second grouping key type
   * @tparam C the third grouping key type
   * @param f1 extracts the first grouping key
   * @param f2 extracts the second grouping key
   * @param f3 extracts the third grouping key
   * */
  def groupBy[A, B, C](
    f1: Out => A,
    f2: Out => B,
    f3: Out => C
  ): GroupedQuery3[In, Out, A, B, C] =
    macro GroupBySyntaxMacro.groupBy3Impl[In, Out, A, B, C]
}
