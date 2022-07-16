package scalaql

import izumi.reflect.macrortti.LightTypeTag
import scalaql.internal.FatalExceptions
import scalaql.utils.TupleFlatten

sealed trait GroupedRollupQuery[In, Out] {
  protected def source: Query[In, Out]

  protected def groupingTags: List[LightTypeTag]

  protected def groupByString: String = {
    val groups = Query.tagsToString(groupingTags)
    s"GROUP BY ROLLUP$groups"
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

final class GroupedRollupQuery1[In: Tag, Out: Tag, G: Tag: Ordering, Fill: Tag] private[scalaql] (
  override protected val source: Query[In, Out],
  group:                         Out => G,
  groupToFill:                   G => Fill,
  defaultFill:                   Fill)
    extends GroupedRollupQuery[In, Out] {

  override protected val groupingTags: List[LightTypeTag] = List(Tag[G].tag)

  def fillna(default: G)(implicit ev: Fill <:< Option[G]): GroupedRollupQuery1[In, Out, G, G] =
    new GroupedRollupQuery1[In, Out, G, G](
      source,
      group,
      identity,
      default
    )

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
    f:                QueryExpressionBuilder[Out] => Aggregation.Of[Out, B]
  )(implicit flatten: TupleFlatten[(Fill, B)]
  ): Query[In, flatten.Out] =
    new Query.AggregateRollupQuery[In, Out, B, flatten.Out](
      source,
      out =>
        List(
          group(out)
        ),
      f,
      List(
        new Query.RollupGroup(asAnyFunc(groupToFill), defaultFill, asAnyOrdering[G])
      ),
      groupByString,
      extractFromList { case List(fill: Fill, b: B) => flatten((fill, b)) }
    )(Tag[In], Tag[B], flatten.tag)
}

final class GroupedRollupQuery2[
  In: Tag,
  Out: Tag,
  G1: Tag: Ordering,
  G2: Tag: Ordering,
  Fill1: Tag,
  Fill2: Tag
] private[scalaql] (
  override protected val source: Query[In, Out],
  group1:                        Out => G1,
  group2:                        Out => G2,
  group1ToFill:                  G1 => Fill1,
  group2ToFill:                  G2 => Fill2,
  defaultFill1:                  Fill1,
  defaultFill2:                  Fill2)
    extends GroupedRollupQuery[In, Out] {

  override protected val groupingTags: List[LightTypeTag] = List(Tag[G1].tag, Tag[G2].tag)

  def fillna(
    default1:     G1,
    default2:     G2
  )(implicit ev1: Fill1 <:< Option[G1],
    ev2:          Fill2 <:< Option[G2]
  ): GroupedRollupQuery2[In, Out, G1, G2, G1, G2] =
    new GroupedRollupQuery2[In, Out, G1, G2, G1, G2](
      source,
      group1,
      group2,
      identity,
      identity,
      default1,
      default2
    )

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
    f:                QueryExpressionBuilder[Out] => Aggregation.Of[Out, B]
  )(implicit flatten: TupleFlatten[((Fill1, Fill2), B)]
  ): Query[In, flatten.Out] =
    new Query.AggregateRollupQuery[In, Out, B, flatten.Out](
      source,
      out =>
        List(
          group1(out),
          group2(out)
        ),
      f,
      List(
        new Query.RollupGroup(asAnyFunc(group1ToFill), defaultFill1, asAnyOrdering[G1]),
        new Query.RollupGroup(asAnyFunc(group2ToFill), defaultFill2, asAnyOrdering[G2])
      ),
      groupByString,
      extractFromList { case List(fill1: Fill1, fill2: Fill2, b: B) => flatten(((fill1, fill2), b)) }
    )(Tag[In], Tag[B], flatten.tag)
}

final class GroupedRollupQuery3[
  In: Tag,
  Out: Tag,
  G1: Tag: Ordering,
  G2: Tag: Ordering,
  G3: Tag: Ordering,
  Fill1: Tag,
  Fill2: Tag,
  Fill3: Tag
] private[scalaql] (
  override protected val source: Query[In, Out],
  group1:                        Out => G1,
  group2:                        Out => G2,
  group3:                        Out => G3,
  group1ToFill:                  G1 => Fill1,
  group2ToFill:                  G2 => Fill2,
  group3ToFill:                  G3 => Fill3,
  defaultFill1:                  Fill1,
  defaultFill2:                  Fill2,
  defaultFill3:                  Fill3)
    extends GroupedRollupQuery[In, Out] {

  override protected val groupingTags: List[LightTypeTag] = List(Tag[G1].tag, Tag[G2].tag, Tag[G3].tag)

  def fillna(
    default1:     G1,
    default2:     G2,
    default3:     G3
  )(implicit ev1: Fill1 <:< Option[G1],
    ev2:          Fill2 <:< Option[G2],
    ev3:          Fill3 <:< Option[G3]
  ): GroupedRollupQuery3[In, Out, G1, G2, G3, G1, G2, G3] =
    new GroupedRollupQuery3[In, Out, G1, G2, G3, G1, G2, G3](
      source,
      group1,
      group2,
      group3,
      identity,
      identity,
      identity,
      default1,
      default2,
      default3
    )

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
    f:                QueryExpressionBuilder[Out] => Aggregation.Of[Out, B]
  )(implicit flatten: TupleFlatten[((Fill1, Fill2, Fill3), B)]
  ): Query[In, flatten.Out] =
    new Query.AggregateRollupQuery[In, Out, B, flatten.Out](
      source,
      out =>
        List(
          group1(out),
          group2(out),
          group3(out)
        ),
      f,
      List(
        new Query.RollupGroup(asAnyFunc(group1ToFill), defaultFill1, asAnyOrdering[G1]),
        new Query.RollupGroup(asAnyFunc(group2ToFill), defaultFill2, asAnyOrdering[G2]),
        new Query.RollupGroup(asAnyFunc(group3ToFill), defaultFill3, asAnyOrdering[G3])
      ),
      groupByString,
      extractFromList { case List(fill1: Fill1, fill2: Fill2, fill3: Fill3, b: B) =>
        flatten(((fill1, fill2, fill3), b))
      }
    )(Tag[In], Tag[B], flatten.tag)
}
