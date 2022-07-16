package scalaql

import izumi.reflect.macrortti.LightTypeTag

sealed trait GroupedRollupQuery[In, Out] {
  protected def source: Query[In, Out]

  protected def groupingTags: List[LightTypeTag]

  protected def groupByString: String = {
    val groups = Query.tagsToString(groupingTags)
    s"GROUP BY ROLLUP$groups"
  }

  override final def toString: String =
    QueryExplain.Continuation(source.explain, QueryExplain.Single(groupByString)).toString

}

final class GroupedRollupQuery1[In: Tag, Out: Tag, G: Tag] private[scalaql] (
  override protected val source: Query[In, Out],
  group:                         Out => G)
    extends GroupedRollupQuery[In, Out] {

  override protected val groupingTags: List[LightTypeTag] = List(Tag[G].tag)

  /**
   * Applies the specified aggregation function to a grouped set of values.
   *
   * Example:
   * {{{
   * TBD
   * }}}
   *
   * @tparam B aggregation result type
   * @param f the aggregation
   * @return this query aggregated
   * */
  def aggregate[B: Tag](
    f: QueryExpressionBuilder[Out] => Aggregation.Of[Out, B]
  ): Query[In, B] = ???
}

final class GroupedRollupQueryCombine1
