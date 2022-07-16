package scalaql.syntax

import scalaql.*

final class GroupByRollupSyntax[In, Out](private val self: Query[In, Out]) extends AnyVal {

  /**
   * Entrypoint for performing rollup aggregations on this query.
   *
   * @tparam A grouping key type
   * @param f extracts grouping key
   * */
  def groupByRollup[A: Tag](
    f:           Out => A
  )(implicit In: Tag[In],
    Out:         Tag[Out]
  ): GroupedRollupQuery1[In, Out, A, Option[A]] =
    new GroupedRollupQuery1[In, Out, A, Option[A]](self, f, Some(_), None)

  /**
   * Entrypoint for performing rollup aggregations on this query.
   *
   * @tparam A the first grouping key type
   * @tparam B the second grouping key type
   * @param f1 extracts the first grouping key
   * @param f2 extracts the second grouping key
   * */
  def groupByRollup[A: Tag, B: Tag](
    f1:          Out => A,
    f2:          Out => B
  )(implicit In: Tag[In],
    Out:         Tag[Out]
  ): GroupedRollupQuery2[In, Out, A, B, Option[A], Option[B]] =
    new GroupedRollupQuery2[In, Out, A, B, Option[A], Option[B]](self, f1, f2, Some(_), Some(_), None, None)

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
  def groupByRollup[A: Tag, B: Tag, C: Tag](
    f1:          Out => A,
    f2:          Out => B,
    f3:          Out => C
  )(implicit In: Tag[In],
    Out:         Tag[Out]
  ): GroupedRollupQuery3[In, Out, A, B, C, Option[A], Option[B], Option[C]] =
    new GroupedRollupQuery3[In, Out, A, B, C, Option[A], Option[B], Option[C]](
      self,
      f1,
      f2,
      f3,
      Some(_),
      Some(_),
      Some(_),
      None,
      None,
      None
    )
}
