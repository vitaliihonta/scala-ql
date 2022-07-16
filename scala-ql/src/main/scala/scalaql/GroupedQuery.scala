package scalaql

import izumi.reflect.macrortti.LightTypeTag
import scalaql.utils.TupleFlatten

sealed trait GroupedQuery[In, Out] {
  protected def source: Query[In, Out]

  protected def groupingTags: List[LightTypeTag]

  protected def groupByString: String = {
    val groups = Query.tagsToString(groupingTags)
    s"GROUP BY$groups"
  }

  override final def toString: String =
    QueryExplain.Continuation(source.explain, QueryExplain.Single(groupByString)).toString

}

final class GroupedQuery1[In: Tag, Out: Tag, G: Tag] private[scalaql] (
  override protected val source: Query[In, Out],
  group:                         Out => G)
    extends GroupedQuery[In, Out] {

  override protected val groupingTags: List[LightTypeTag] = List(Tag[G].tag)

  /**
   * Applies the specified aggregation function to a grouped set of values.
   *
   * Example:
   * {{{
   *   case class Statistics(
   *     country: String, 
   *     population: Int, 
   *     averageAge: Double
   *   )
   *
   *   select[Person]
   *     .groupBy(_.country)
   *     .aggregate(person =>
   *       person.size &&
   *       person.avgBy(_.age.toDouble)
   *     )
   *     .mapTo(Statistics)
   * }}}
   *
   * @tparam B aggregation result type
   * @param f the aggregation
   * @return this query aggregated
   * */
  def aggregate[B: Tag](
    f:                QueryExpressionBuilder[Out] => Aggregation.Of[Out, B]
  )(implicit flatten: TupleFlatten[(G, B)]
  ): Query[In, flatten.Out] =
    new Query.AggregateQuery[In, Out, G, B, flatten.Out](
      source,
      group,
      f,
      groupByString,
      flatten
    )
}

final class GroupedQuery2[In: Tag, Out: Tag, G1: Tag, G2: Tag] private[scalaql] (
  override protected val source: Query[In, Out],
  group1:                        Out => G1,
  group2:                        Out => G2)
    extends GroupedQuery[In, Out] {

  override protected val groupingTags: List[LightTypeTag] = List(Tag[G1].tag, Tag[G2].tag)

  /**
   * Applies the specified aggregation function to a grouped set of values.
   *
   * Example:
   * {{{
   *   case class Statistics(
   *     country: String,
   *     city: String,
   *     population: Int, 
   *     averageAge: Double
   *   )
   *
   *   select[Person]
   *     .groupBy(_.country, _.city)
   *     .aggregate(person =>
   *       person.size &&
   *       person.avgBy(_.age.toDouble)
   *     )
   *     .mapTo(Statistics)
   * }}}
   *
   * @tparam B aggregation result type
   * @param f the aggregation
   * @return this query aggregated
   * */
  def aggregate[B: Tag](
    f:                QueryExpressionBuilder[Out] => Aggregation.Of[Out, B]
  )(implicit flatten: TupleFlatten[((G1, G2), B)]
  ): Query[In, flatten.Out] =
    new Query.AggregateQuery[In, Out, (G1, G2), B, flatten.Out](
      source,
      out => (group1(out), group2(out)),
      f,
      groupByString,
      flatten
    )
}

final class GroupedQuery3[In: Tag, Out: Tag, G1: Tag, G2: Tag, G3: Tag] private[scalaql] (
  override protected val source: Query[In, Out],
  group1:                        Out => G1,
  group2:                        Out => G2,
  group3:                        Out => G3)
    extends GroupedQuery[In, Out] {

  override protected val groupingTags: List[LightTypeTag] = List(Tag[G1].tag, Tag[G2].tag, Tag[G3].tag)

  /**
   * Applies the specified aggregation function to a grouped set of values.
   *
   * Example:
   * {{{
   *   case class Statistics(
   *     country: String,
   *     city: String,
   *     profession: String,
   *     population: Int, 
   *     averageAge: Double
   *   )
   *
   *   select[Person]
   *     .groupBy(_.country, _.city, _.profession)
   *     .aggregate(person =>
   *       person.size &&
   *       person.avgBy(_.age.toDouble)
   *     )
   *     .mapTo(Statistics)
   * }}}
   *
   * @tparam B aggregation result type
   * @param f the aggregation
   * @return this query aggregated
   * */
  def aggregate[B: Tag](
    f:                QueryExpressionBuilder[Out] => Aggregation.Of[Out, B]
  )(implicit flatten: TupleFlatten[((G1, G2, G3), B)]
  ): Query[In, flatten.Out] =
    new Query.AggregateQuery[In, Out, (G1, G2, G3), B, flatten.Out](
      source,
      out => (group1(out), group2(out), group3(out)),
      f,
      groupByString,
      flatten
    )
}
