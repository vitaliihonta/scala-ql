package scalaql

import izumi.reflect.macrortti.LightTypeTag
import scalaql.internal.FatalExceptions
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

  protected def asAnyOrdering[A: Ordering]: Ordering[Any] = Ordering[A].asInstanceOf[Ordering[Any]]
  protected def asAnyFunc[A, B](f: A => B): Any => Any    = f.asInstanceOf[Any => Any]

  protected def extractFromList[U](pf: PartialFunction[List[Any], U]): List[Any] => U = { values =>
    def error(info: String) = FatalExceptions.libraryError(s"Unable to extract tuple from list: $info")

    try
      pf.applyOrElse[List[Any], U](values, _ => throw error(s"number of arguments didn't match. Actual: $values"))
    catch {
      case cce: ClassCastException =>
        throw error(s"argument types didn't match: $cce")
    }
  }
}

final class GroupedQuery1[In, Out, F] private[scalaql] (
  override protected val source: Query[In, Out],
  group:                         Out => Any,
  kind:                          Query.GroupKind[Any, F]
)(implicit In:                   Tag[In],
  Out:                           Tag[Out],
  F:                             Tag[F])
    extends GroupedQuery[In, Out] {

  override protected val groupingTags: List[LightTypeTag] =
    List(Tag[F].tag)

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
  )(implicit flatten: TupleFlatten[(F, B)]
  ): Query[In, flatten.Out] =
    new Query.AggregateQuery[In, Out, B, flatten.Out](
      source,
      out =>
        Query.GroupKeys(
          Map(
            0 -> Query.GroupKey(group(out), kind).widen
          ),
          Map.empty
        ),
      List(kind.widen),
      f,
      groupByString,
      extractFromList { case List(fill: F, b: B) => flatten((fill, b)) }
    )
}

final class GroupedQuery2[In, Out, F1, F2] private[scalaql] (
  override protected val source: Query[In, Out],
  group1:                        Out => Any,
  group2:                        Out => Any,
  kind1:                         Query.GroupKind[Any, F1],
  kind2:                         Query.GroupKind[Any, F2]
)(implicit In:                   Tag[In],
  Out:                           Tag[Out],
  F1:                            Tag[F1],
  F2:                            Tag[F2])
    extends GroupedQuery[In, Out] {

  override protected val groupingTags: List[LightTypeTag] =
    List(Tag[F1].tag, Tag[F2].tag)

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
  )(implicit flatten: TupleFlatten[((F1, F2), B)]
  ): Query[In, flatten.Out] =
    new Query.AggregateQuery[In, Out, B, flatten.Out](
      source,
      out =>
        Query.GroupKeys(
          Map(
            0 -> Query.GroupKey(group1(out), kind1).widen,
            1 -> Query.GroupKey(group2(out), kind2).widen
          ),
          Map.empty
        ),
      List(kind1.widen, kind2.widen),
      f,
      groupByString,
      extractFromList { case List(fill1: F1, fill2: F2, b: B) => flatten(((fill1, fill2), b)) }
    )
}

final class GroupedQuery3[In, Out, F1, F2, F3] private[scalaql] (
  override protected val source: Query[In, Out],
  group1:                        Out => Any,
  group2:                        Out => Any,
  group3:                        Out => Any,
  val kind1:                     Query.GroupKind[Any, F1],
  val kind2:                     Query.GroupKind[Any, F2],
  val kind3:                     Query.GroupKind[Any, F3]
)(implicit In:                   Tag[In],
  Out:                           Tag[Out],
  F1:                            Tag[F1],
  F2:                            Tag[F2],
  F3:                            Tag[F3])
    extends GroupedQuery[In, Out] {

  override protected val groupingTags: List[LightTypeTag] =
    List(Tag[F1].tag, Tag[F2].tag, Tag[F3].tag)

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
  )(implicit flatten: TupleFlatten[((F1, F2, F3), B)]
  ): Query[In, flatten.Out] =
    new Query.AggregateQuery[In, Out, B, flatten.Out](
      source,
      out =>
        Query.GroupKeys(
          Map(
            0 -> Query.GroupKey(group1(out), kind1).widen,
            1 -> Query.GroupKey(group2(out), kind2).widen,
            2 -> Query.GroupKey(group3(out), kind3).widen
          ),
          Map.empty
        ),
      List(kind1.widen, kind2.widen, kind3.widen),
      f,
      groupByString,
      extractFromList { case List(fill1: F1, fill2: F2, fill3: F3, b: B) =>
        flatten(((fill1, fill2, fill3), b))
      }
    )
}
