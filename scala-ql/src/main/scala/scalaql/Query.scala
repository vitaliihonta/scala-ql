package scalaql

import izumi.reflect.macrortti.LightTypeTag
import scalaql.internal.PartialFunctionAndThenCompat
import scalaql.utils.TupleFlatten
import spire.algebra.Order
import scala.annotation.unchecked.uncheckedVariance

sealed abstract class Query[-In: Tag, +Out: Tag] extends Serializable {

  def explain: QueryExplain

  override final def toString: String = explain.toString

  def map[B: Tag](f: Out => B): Query[In, B] =
    new Query.MapQuery[In, Out, B](this, f, Tag[B].tag)

  def collect[B: Tag](pf: PartialFunction[Out, B]): Query[In, B] =
    new Query.CollectQuery[In, Out, B](this, pf, Tag[B].tag)

  def where(p: Predicate[Out]): Query[In, Out] =
    new Query.WhereQuery[In, Out](this, p)

  def withFilter(p: Predicate[Out]): Query[In, Out] = where(p)

  def whereNot(p: Predicate[Out]): Query[In, Out] =
    new Query.WhereQuery[In, Out](this, !p(_), nameHint = Some("WHERE_NOT"))

  def mapConcat[B: Tag](f: Out => Iterable[B]): Query[In, B] =
    new Query.FlatMapQuery[In, Out, B](
      this,
      out => new Query.Const[B](f(out)),
      Tag[In].tag,
      Tag[B].tag,
      nameHint = Some("MAP_CONCAT")
    )

  def flatMap[In2 <: In: Tag, B: Tag](f: Out => Query[In2, B]): Query[In2, B] =
    new Query.FlatMapQuery[In2, Out, B](this, f, Tag[In2].tag, Tag[B].tag)

  def whereSubQuery[In2 <: In: Tag](p: Out => QueryResult[In2, Boolean]): Query[In2, Out] =
    new Query.WhereSubQuery[In2, Out](this, p)

  def accumulate[S: Tag, B: Tag](
    initialState: S
  )(modifyState:  (S, Out) => S
  )(getResults:   S => Iterable[B]
  ): Query[In, B] =
    new Query.Accumulate[In, Out, S, B](
      this,
      initialState,
      modifyState,
      getResults,
      Tag[S].tag
    )

  def statefulMap[S: Tag, B: Tag](
    initialState: S
  )(process:      (S, Out) => (S, B)
  ): Query[In, B] =
    new Query.StatefulMapConcat[In, Out, S, B](
      this,
      initialState,
      process = { (state, out) =>
        val (newState, next) = process(state, out)
        newState -> List(next)
      },
      Tag[S].tag,
      nameHint = Some(s"STATEFUL_MAP($initialState)")
    )

  def statefulMapConcat[S: Tag, B: Tag](
    initialState: S
  )(process:      (S, Out) => (S, Iterable[B])
  ): Query[In, B] = new Query.StatefulMapConcat[In, Out, S, B](
    this,
    initialState,
    process,
    Tag[S].tag,
    nameHint = None
  )

  def deduplicate: Query[In, Out] = deduplicateBy(identity[Out])

  def deduplicateBy[K: Tag](f: Out => K): Query[In, Out] =
    new Query.StatefulMapConcat[In, Out, Set[K], Out](
      this,
      initialState = Set.empty[K],
      process = { (keys, out) =>
        val key = f(out)
        if (keys.contains(key)) keys -> Nil
        else (keys + key)            -> List(out)
      },
      Tag[Set[K]].tag,
      nameHint = Some(s"DEDUPLICATE BY(${Tag[K].tag})")
    )

  final def ++[In2 <: In: Tag, Out0 >: Out: Tag](that: Query[In2, Out0]): Query[In2, Out0] =
    union(that)

  def union[In2 <: In: Tag, Out0 >: Out: Tag](that: Query[In2, Out0]): Query[In2, Out0] =
    new Query.UnionQuery[In2, Out0](this, that)

  final def >>>[Out0 >: Out: Tag, Out2: Tag](that: Query[From[Out0], Out2]): Query[In, Out2] =
    andThen(that)

  def andThen[Out0 >: Out: Tag, Out2: Tag](that: Query[From[Out0], Out2]): Query[In, Out2] =
    new Query.AndThenQuery[In, Out0, Out2](this, that, Tag[Out0].tag)

  def groupBy[A: Tag](f: GroupBy[Out, A]): Query.GroupByQuery[In, Out, A] =
    new Query.GroupByQueryImpl[In, Out, A](this, f, List(Tag[A].tag))

  def groupBy[A: Tag, B: Tag](f: GroupBy[Out, A], g: GroupBy[Out, B]): Query.GroupByQuery[In, Out, (A, B)] =
    new Query.GroupByQueryImpl[In, Out, (A, B)](this, x => (f(x), g(x)), List(Tag[A].tag, Tag[B].tag))

  def groupBy[A: Tag, B: Tag, C: Tag](
    f: GroupBy[Out, A],
    g: GroupBy[Out, B],
    h: GroupBy[Out, C]
  ): Query.GroupByQuery[In, Out, (A, B, C)] =
    new Query.GroupByQueryImpl[In, Out, (A, B, C)](
      this,
      x => (f(x), g(x), h(x)),
      List(Tag[A].tag, Tag[B].tag, Tag[C].tag)
    )
}

object Query {

  final class Const[A: Tag](private[scalaql] val values: Iterable[A]) extends Query[Any, A] {
    override def explain: QueryExplain = {
      val truncatedValues = truncateToString(values)
      QueryExplain.Single(s"CONST($truncatedValues)")
    }
  }

  final class FromQuery[A: Tag] extends Query[From[A], A] {
    private[scalaql] val inputTag: LightTypeTag = Tag[A].tag

    override def explain: QueryExplain = QueryExplain.Single(s"FROM($inputTag)")
  }

  final class AliasedQuery[In: Tag, U: Tag](
    private[scalaql] val inputInfo:       LightTypeTag,
    private[scalaql] val inputAliasedTag: LightTypeTag,
    private[scalaql] val alias:           LightTypeTag)
      extends Query[From[In as U], In] {

    override def explain: QueryExplain = QueryExplain.Single(s"FROM($inputInfo AS ${alias.shortName})")
  }

  final class MapQuery[In: Tag, Out0, Out1: Tag](
    private[scalaql] val source:  Query[In, Out0],
    private[scalaql] val project: Out0 => Out1,
    private[scalaql] val outTag:  LightTypeTag)
      extends Query[In, Out1] {

    override def map[B: Tag](f: Out1 => B): Query[In, B] =
      new MapQuery[In, Out0, B](source, project andThen f, Tag[B].tag)

    override def explain: QueryExplain =
      QueryExplain.Continuation(source.explain, QueryExplain.Single(s"MAP($outTag)"))
  }

  final class FlatMapQuery[In: Tag, Out0, Out1: Tag](
    private[scalaql] val source:   Query[In, Out0],
    private[scalaql] val projectM: Out0 => Query[In, Out1],
    private[scalaql] val inTag:    LightTypeTag,
    private[scalaql] val outTag:   LightTypeTag,
    nameHint:                      Option[String] = None)
      extends Query[In, Out1] {

    override def map[B: Tag](f: Out1 => B): Query[In, B] =
      new FlatMapQuery[In, Out0, B](source, projectM(_).map(f), inTag, Tag[B].tag)

    override def where(p: Predicate[Out1]): Query[In, Out1] =
      new FlatMapQuery[In, Out0, Out1](source, projectM(_).where(p), inTag, outTag)

    override def explain: QueryExplain =
      QueryExplain.Continuation(
        source.explain,
        QueryExplain.Single(nameHint.getOrElse(s"FLATMAP($inTag => $outTag)"))
      )
  }

  final class WhereQuery[In: Tag, Out: Tag](
    private[scalaql] val source:     Query[In, Out],
    private[scalaql] val filterFunc: Predicate[Out],
    nameHint:                        Option[String] = None)
      extends Query[In, Out] {

    override def where(p: Predicate[Out]): Query[In, Out] =
      new WhereQuery[In, Out](source, out => filterFunc(out) && p(out))

    override def explain: QueryExplain =
      QueryExplain.Continuation(source.explain, QueryExplain.Single(nameHint.getOrElse("WHERE")))
  }

  final class CollectQuery[In: Tag, Out, Out1: Tag](
    private[scalaql] val source:      Query[In, Out],
    private[scalaql] val collectFunc: PartialFunction[Out, Out1],
    private[scalaql] val outTag:      LightTypeTag)
      extends Query[In, Out1] {

    override def collect[B: Tag](pf: PartialFunction[Out1, B]): Query[In, B] =
      new CollectQuery[In, Out, B](
        source,
        PartialFunctionAndThenCompat.andThen(collectFunc, pf),
        Tag[B].tag
      )

    override def where(p: Predicate[Out1]): Query[In, Out1] =
      new CollectQuery[In, Out, Out1](
        source,
        PartialFunctionAndThenCompat.andThen[Out, Out1, Out1](
          collectFunc,
          {
            case out1 if p(out1) => out1
          }
        ),
        outTag
      )

    override def explain: QueryExplain =
      QueryExplain.Continuation(source.explain, QueryExplain.Single(s"COLLECT($outTag)"))
  }

  final class WhereSubQuery[In: Tag, Out: Tag](
    private[scalaql] val source:    Query[In, Out],
    private[scalaql] val predicate: Out => QueryResult[In, Boolean])
      extends Query[In, Out] {

    override def where(p: Predicate[Out]): Query[In, Out] =
      new WhereSubQuery[In, Out](source, x => predicate(x).map(_ && p(x)))

    override def explain: QueryExplain =
      QueryExplain.Continuation(source.explain, QueryExplain.Single("WHERE SUBQUERY"))

    override def whereSubQuery[In2 <: In: Tag](p: Out => QueryResult[In2, Boolean]): Query[In2, Out] =
      new WhereSubQuery[In2, Out](
        source,
        out =>
          predicate(out).flatMap {
            case false => QueryResult.const(false)
            case true  => p(out)
          }
      )
  }

  final class StatefulMapConcat[In: Tag, Out, S, B: Tag](
    private[scalaql] val source:       Query[In, Out],
    private[scalaql] val initialState: S,
    private[scalaql] val process:      (S, Out) => (S, Iterable[B]),
    private[scalaql] val stateTag:     LightTypeTag,
    nameHint:                          Option[String])
      extends Query[In, B] {

    override def explain: QueryExplain = {
      val truncatedState = truncateToString(initialState)
      QueryExplain.Continuation(
        source.explain,
        QueryExplain.Single(
          nameHint.getOrElse(s"STATEFUL MAP CONCAT(initial: $stateTag = $truncatedState => ${Tag[B].tag})")
        )
      )
    }

    // TODO: optimize
  }

  final class Accumulate[In: Tag, Out, S, B: Tag](
    private[scalaql] val source:       Query[In, Out],
    private[scalaql] val initialState: S,
    private[scalaql] val modifyState:  (S, Out) => S,
    private[scalaql] val getResults:   S => Iterable[B],
    private[scalaql] val stateTag:     LightTypeTag)
      extends Query[In, B] {

    override def explain: QueryExplain = {
      val truncatedState = truncateToString(initialState)
      QueryExplain.Continuation(
        source.explain,
        QueryExplain.Single(s"ACCUMULATE(initial: $stateTag = $truncatedState => ${Tag[B].tag})")
      )
    }

    override def map[C: Tag](f: B => C): Query[In, C] =
      new Query.Accumulate[In, Out, S, C](
        source,
        initialState,
        modifyState,
        getResults(_).map(f),
        stateTag
      )

    override def mapConcat[C: Tag](f: B => Iterable[C]): Query[In, C] =
      new Query.Accumulate[In, Out, S, C](
        source,
        initialState,
        modifyState,
        getResults(_).flatMap(f),
        stateTag
      )

    override def collect[C: Tag](pf: PartialFunction[B, C]): Query[In, C] =
      new Query.Accumulate[In, Out, S, C](
        source,
        initialState,
        modifyState,
        getResults(_).collect(pf),
        stateTag
      )

    override def where(p: Predicate[B]): Query[In, B] =
      new Query.Accumulate[In, Out, S, B](
        source,
        initialState,
        modifyState,
        getResults(_).filter(p),
        stateTag
      )
  }

  final class SortByQuery[In: Tag, Out: Tag, By](
    private[scalaql] val source:         Query[In, Out],
    private[scalaql] val sortBy:         SortBy[Out, By],
    private[scalaql] val sortingTag:     Option[LightTypeTag]
  )(private[scalaql] implicit val order: Order[By])
      extends Query[In, Out] {

    override def explain: QueryExplain = {
      val sortBy = sortingTag.fold(ifEmpty = "")(by => s" BY($by)")
      QueryExplain.Continuation(
        source.explain,
        QueryExplain.Single(s"SORT$sortBy")
      )
    }
  }

  final class AggregateQuery[In: Tag, Out0, G, Out1, Out2: Tag](
    private[scalaql] val source:        Query[In, Out0],
    private[scalaql] val group:         GroupBy[Out0, G],
    private[scalaql] val agg:           Aggregate[G, Out0, Out1],
    private[scalaql] val tupled:        TupleFlatten.Aux[Out1, Out2],
    private[scalaql] val groupByString: String)
      extends Query[In, Out2] {

    override def explain: QueryExplain =
      QueryExplain.Continuation(
        source.explain,
        QueryExplain.Continuation(
          QueryExplain.Single(groupByString),
          QueryExplain.Single(s"AGGREGATE(${Tag[Out2].tag})")
        )
      )
  }

  sealed trait GroupByQuery[-In, +Out, +G] {

    def aggregate[B](
      f:               Aggregate[G, Out, B] @uncheckedVariance
    )(implicit tupled: TupleFlatten[B]
    ): Query[In, tupled.Out]
  }

  final class GroupByQueryImpl[In: Tag, Out: Tag, G](
    private[scalaql] val source:       Query[In, Out],
    private[scalaql] val group:        GroupBy[Out, G],
    private[scalaql] val groupingTags: List[LightTypeTag])
      extends GroupByQuery[In, Out, G] {

    override def aggregate[B](
      f:               Aggregate[G, Out, B]
    )(implicit tupled: TupleFlatten[B]
    ): Query[In, tupled.Out] = {
      implicit val outTag: Tag[tupled.Out] = tupled.tag
      new AggregateQuery[In, Out, G, B, tupled.Out](
        source,
        group,
        f,
        tupled,
        groupByString
      )
    }

    private def groupByString: String = {
      val groups = tagsToString(groupingTags)
      s"GROUP BY$groups"
    }

    override def toString: String =
      QueryExplain.Continuation(source.explain, QueryExplain.Single(groupByString)).toString
  }

  final class InnerJoinPartiallyApplied[In <: From[?]: Tag, In2 <: From[?]: Tag, Out: Tag, Out2: Tag](
    left:     Query[In, Out],
    right:    Query[In2, Out2],
    joinType: InnerJoinType) {

    def on(f: (Out, Out2) => Boolean): Query[In & In2, (Out, Out2)] =
      new InnerJoinedQuery[In, In2, Out, Out2](left, right, joinType, f)
  }

  private[scalaql] sealed abstract class InnerJoinType(override val toString: String)
  private[scalaql] case object InnerJoin extends InnerJoinType("INNER")
  private[scalaql] case object CrossJoin extends InnerJoinType("CROSS")

  sealed abstract class JoinedQuery[In <: From[?]: Tag, In2 <: From[?]: Tag, Out, Out2, Res: Tag]
      extends Query[In & In2, Res] {
    private[scalaql] val left: Query[In, Out]
    private[scalaql] val right: Query[In2, Out2]
  }

  final class InnerJoinedQuery[In <: From[?]: Tag, In2 <: From[?]: Tag, Out: Tag, Out2: Tag](
    private[scalaql] val left:     Query[In, Out],
    private[scalaql] val right:    Query[In2, Out2],
    private[scalaql] val joinType: InnerJoinType,
    private[scalaql] val on:       (Out, Out2) => Boolean)
      extends JoinedQuery[In, In2, Out, Out2, (Out, Out2)] {

    override def explain: QueryExplain =
      QueryExplain.Operation(
        left.explain,
        right.explain,
        s"$joinType JOIN"
      )
  }

  final class LeftJoinPartiallyApplied[In <: From[?]: Tag, In2 <: From[?]: Tag, Out: Tag, Out2: Tag](
    left:  Query[In, Out],
    right: Query[In2, Out2]) {

    def on(f: (Out, Out2) => Boolean): Query[In & In2, (Out, Option[Out2])] =
      new LeftJoinedQuery[In, In2, Out, Out2](left, right, f)
  }

  final class LeftJoinedQuery[In <: From[?]: Tag, In2 <: From[?]: Tag, Out: Tag, Out2: Tag](
    private[scalaql] val left:  Query[In, Out],
    private[scalaql] val right: Query[In2, Out2],
    private[scalaql] val on:    (Out, Out2) => Boolean)
      extends JoinedQuery[In, In2, Out, Out2, (Out, Option[Out2])] {

    override def explain: QueryExplain =
      QueryExplain.Operation(
        left.explain,
        right.explain,
        "LEFT JOIN"
      )

  }

  final class UnionQuery[In: Tag, Out: Tag](
    private[scalaql] val left:  Query[In, Out],
    private[scalaql] val right: Query[In, Out])
      extends Query[In, Out] {

    override def where(p: Predicate[Out]): Query[In, Out] =
      new UnionQuery[In, Out](left.where(p), right.where(p))

    override def map[B: Tag](f: Out => B): Query[In, B] =
      new UnionQuery[In, B](left.map(f), right.map(f))

    override def collect[B: Tag](pf: PartialFunction[Out, B]): Query[In, B] =
      new UnionQuery[In, B](left.collect(pf), right.collect(pf))

    override def union[In2 <: In: Tag, Out0 >: Out: Tag](that: Query[In2, Out0]): Query[In2, Out0] =
      new UnionQuery[In2, Out0](left, right union that)

    override def whereSubQuery[In2 <: In: Tag](p: Out => QueryResult[In2, Boolean]): Query[In2, Out] =
      new UnionQuery[In2, Out](left.whereSubQuery(p), right.whereSubQuery(p))

    override def explain: QueryExplain =
      QueryExplain.Operation(
        left.explain,
        right.explain,
        "UNION"
      )
  }

  final class AndThenQuery[In0: Tag, OutA, OutB: Tag](
    private[scalaql] val left:    Query[In0, OutA],
    private[scalaql] val right:   Query[From[OutA], OutB],
    private[scalaql] val outATag: LightTypeTag)
      extends Query[In0, OutB] {

    override def explain: QueryExplain =
      QueryExplain.Operation(
        left.explain,
        right.explain,
        "AND THEN"
      )

    override def andThen[Out0 >: OutB: Tag, Out2: Tag](that: Query[From[Out0], Out2]): Query[In0, Out2] =
      new AndThenQuery[In0, OutA, Out2](
        left,
        right >>> that,
        outATag
      )

    override def map[B: Tag](f: OutB => B): Query[In0, B] =
      new AndThenQuery[In0, OutA, B](
        left,
        right.map(f),
        Tag[B].tag
      )

    override def where(p: Predicate[OutB]): Query[In0, OutB] =
      new AndThenQuery[In0, OutA, OutB](
        left,
        right.where(p),
        outATag
      )

    override def collect[B: Tag](pf: PartialFunction[OutB, B]): Query[In0, B] =
      new AndThenQuery[In0, OutA, B](
        left,
        right.collect(pf),
        Tag[B].tag
      )
  }

  private def tagsToString(tags: List[LightTypeTag]): String =
    tags.mkString("(", ", ", ")")

  private val DefaultStrLength: Int = 20
  private def truncateToString(value: Any): String = {
    val str = value.toString
    if (str.length <= DefaultStrLength) str
    else s"${str.take(DefaultStrLength)}... (${str.length - DefaultStrLength} symbols omitted)"
  }
}
