package scalaql

import izumi.reflect.Tag
import izumi.reflect.macrortti.LightTypeTag
import scalaql.internal.PartialFunctionAndThenCompat
import scalaql.utils.TupleFlatten
import spire.algebra.Order
import scala.annotation.unchecked.uncheckedVariance

sealed trait Query[-In, +Out] {

  def map[B](f: Out => B): Query[In, B] =
    new Query.MapQuery[In, Out, B](this, f)

  def collect[B](pf: PartialFunction[Out, B]): Query[In, B] =
    new Query.CollectQuery[In, Out, B](this, pf)

  def where(p: Predicate[Out]): Query[In, Out] =
    new Query.WhereQuery[In, Out](this, p)

  def withFilter(p: Predicate[Out]): Query[In, Out] = where(p)

  def whereNot(p: Predicate[Out]): Query[In, Out] =
    new Query.WhereQuery[In, Out](this, !p(_), nameHint = Some("WHERE_NOT"))

  def mapConcat[B](f: Out => Iterable[B])(implicit In: Tag[In] @uncheckedVariance): Query[In, B] =
    new Query.FlatMapQuery[In, Out, B](
      this,
      out => new Query.Const[B](f(out)),
      In.tag,
      nameHint = Some("MAP_CONCAT")
    )

  def flatMap[In2 <: In: Tag, B](f: Out => Query[In2, B]): Query[In2, B] =
    new Query.FlatMapQuery[In2, Out, B](this, f, Tag[In2].tag)

  def whereSubQuery[In2 <: In](p: Out => QueryResult[In2, Boolean]): Query[In2, Out] =
    new Query.WhereSubQuery[In2, Out](this, p)

  def accumulate[S, B](
    initialState: S
  )(modifyState:  (S, Out) => S
  )(getResults:   S => Iterable[B]
  ): Query[In, B] =
    new Query.Accumulate[In, Out, S, B](this, initialState, modifyState, getResults)

  def statefulMap[S, B](
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
      nameHint = Some(s"STATEFUL_MAP($initialState)")
    )

  def statefulMapConcat[S, B](
    initialState: S
  )(process:      (S, Out) => (S, Iterable[B])
  ): Query[In, B] = new Query.StatefulMapConcat[In, Out, S, B](this, initialState, process, nameHint = None)

  def deduplicate: Query[In, Out] = deduplicateBy(identity[Out])

  def deduplicateBy[K](f: Out => K): Query[In, Out] =
    new Query.StatefulMapConcat[In, Out, Set[K], Out](
      this,
      initialState = Set.empty[K],
      process = { (keys, out) =>
        val key = f(out)
        if (keys.contains(key)) keys -> Nil
        else (keys + key)            -> List(out)
      },
      nameHint = Some("DEDUPLICATE")
    )

  def ++[In2 <: In, Out0 >: Out](that: Query[In2, Out0]): Query[In2, Out0] =
    union(that)

  def union[In2 <: In, Out0 >: Out](that: Query[In2, Out0]): Query[In2, Out0] =
    new Query.UnionQuery[In2, Out0](this, that)

  def >>>[Out0 >: Out: Tag, Out2](that: Query[From[Out0], Out2]): Query[In, Out2] =
    andThen(that)

  def andThen[Out0 >: Out: Tag, Out2](that: Query[From[Out0], Out2]): Query[In, Out2] =
    new Query.AndThenQuery[In, Out0, Out2](this, that, Tag[Out0].tag)

  def join[In2 <: In, Out2](
    that: Query[In2, Out2]
  ): Query.InnerJoinPartiallyApplied[In2, Out, Out2] @uncheckedVariance =
    new Query.InnerJoinPartiallyApplied[In2, Out, Out2](this, that, Query.InnerJoin)

  def crossJoin[In2 <: In, Out2](
    that: Query[In2, Out2]
  ): Query.InnerJoinPartiallyApplied[In2, Out, Out2] @uncheckedVariance =
    new Query.InnerJoinPartiallyApplied[In2, Out, Out2](this, that, Query.CrossJoin)

  def leftJoin[In2 <: In, Out2](
    that: Query[In2, Out2]
  ): Query.LeftJoinPartiallyApplied[In2, Out, Out2] @uncheckedVariance =
    new Query.LeftJoinPartiallyApplied[In2, Out, Out2](this, that)

  def sorted(implicit order: Order[Out] @uncheckedVariance): Query[In, Out] =
    sortBy(identity)

  def sortBy[B: Order](f: SortBy[Out, B]): Query[In, Out] =
    new Query.SortByQuery[In, Out, B](this, f)

  def groupBy[A](f: GroupBy[Out, A]): Query.GroupByQuery[In, Out, A] =
    new Query.GroupByQueryImpl[In, Out, A](this, f)

  def groupBy[A, B](f: GroupBy[Out, A], g: GroupBy[Out, B]): Query.GroupByQuery[In, Out, (A, B)] =
    new Query.GroupByQueryImpl[In, Out, (A, B)](this, x => (f(x), g(x)))

  def groupBy[A, B, C](
    f: GroupBy[Out, A],
    g: GroupBy[Out, B],
    h: GroupBy[Out, C]
  ): Query.GroupByQuery[In, Out, (A, B, C)] =
    new Query.GroupByQueryImpl[In, Out, (A, B, C)](
      this,
      x => (f(x), g(x), h(x))
    )
}

object Query {

  final class Const[A](private[scalaql] val values: Iterable[A]) extends Query[Any, A] {
    override def toString: String = s"CONST($values)"
  }

  final class FromQuery[A](private[scalaql] val inputTag: LightTypeTag) extends Query[From[A], A] {
    override def toString: String = s"FROM(${inputTag.shortName})"
  }

  final class AliasedQuery[In, U](
    private[scalaql] val inputInfo:       LightTypeTag,
    private[scalaql] val inputAliasedTag: LightTypeTag,
    private[scalaql] val alias:           LightTypeTag)
      extends Query[From[@@[In, U]], In] {

    override def toString: String = s"FROM($inputInfo AS ${alias.shortName})"
  }

  final class MapQuery[In, Out0, Out1](
    private[scalaql] val source:  Query[In, Out0],
    private[scalaql] val project: Out0 => Out1)
      extends Query[In, Out1] {

    override def map[B](f: Out1 => B): Query[In, B] =
      new MapQuery[In, Out0, B](source, project andThen f)

    override def toString: String = s"$source -> MAP"
  }

  final class FlatMapQuery[In, Out0, Out1](
    private[scalaql] val source:   Query[In, Out0],
    private[scalaql] val projectM: Out0 => Query[In, Out1],
    private[scalaql] val inTag:    LightTypeTag,
    nameHint:                      Option[String] = None)
      extends Query[In, Out1] {

    override def map[B](f: Out1 => B): Query[In, B] =
      new FlatMapQuery[In, Out0, B](source, projectM(_).map(f), inTag)

    override def where(p: Predicate[Out1]): Query[In, Out1] =
      new FlatMapQuery[In, Out0, Out1](source, projectM(_).where(p), inTag)

    override def toString: String = {
      val description = nameHint.getOrElse(s"FLATMAP($inTag)")
      s"$source -> $description"
    }
  }

  final class WhereQuery[In, Out](
    private[scalaql] val source:     Query[In, Out],
    private[scalaql] val filterFunc: Predicate[Out],
    nameHint:                        Option[String] = None)
      extends Query[In, Out] {

    override def where(p: Predicate[Out]): Query[In, Out] =
      new WhereQuery[In, Out](source, out => filterFunc(out) && p(out))

    override def toString: String = {
      val description = nameHint.getOrElse("WHERE")
      s"$source -> $description"
    }
  }

  final class CollectQuery[In, Out, Out1](
    private[scalaql] val source:      Query[In, Out],
    private[scalaql] val collectFunc: PartialFunction[Out, Out1])
      extends Query[In, Out1] {

    override def collect[B](pf: PartialFunction[Out1, B]): Query[In, B] =
      new CollectQuery[In, Out, B](
        source,
        PartialFunctionAndThenCompat.andThen(collectFunc, pf)
      )

    override def where(p: Predicate[Out1]): Query[In, Out1] =
      new CollectQuery[In, Out, Out1](
        source,
        PartialFunctionAndThenCompat.andThen[Out, Out1, Out1](
          collectFunc,
          {
            case out1 if p(out1) => out1
          }
        )
      )

    override def toString: String = s"$source -> COLLECT"
  }

  final class WhereSubQuery[In, Out](
    private[scalaql] val source:    Query[In, Out],
    private[scalaql] val predicate: Out => QueryResult[In, Boolean])
      extends Query[In, Out] {

    override def where(p: Predicate[Out]): Query[In, Out] =
      new WhereSubQuery[In, Out](source, x => predicate(x).map(_ && p(x)))

    override def toString: String = s"$source -> WHERE_SUBQUERY"

    override def whereSubQuery[In2 <: In](p: Out => QueryResult[In2, Boolean]): Query[In2, Out] =
      new WhereSubQuery[In2, Out](
        source,
        out =>
          predicate(out).flatMap {
            case false => QueryResult.const(false)
            case true  => p(out)
          }
      )
  }

  final class StatefulMapConcat[In, Out, S, B](
    private[scalaql] val source:       Query[In, Out],
    private[scalaql] val initialState: S,
    private[scalaql] val process:      (S, Out) => (S, Iterable[B]),
    nameHint:                          Option[String])
      extends Query[In, B] {

    override def toString: String = {
      val description = nameHint.getOrElse(s"STATEFUL_MAP_CONCAT(initial_state=$initialState)")
      s"$source -> $description"
    }

    // TODO: optimize
  }

  final class Accumulate[In, Out, S, B](
    private[scalaql] val source:       Query[In, Out],
    private[scalaql] val initialState: S,
    private[scalaql] val modifyState:  (S, Out) => S,
    private[scalaql] val getResults:   S => Iterable[B])
      extends Query[In, B] {

    override def toString: String = s"$source -> ACCUMULATE(initial_state=$initialState)"

    override def map[C](f: B => C): Query[In, C] =
      new Query.Accumulate[In, Out, S, C](
        source,
        initialState,
        modifyState,
        getResults(_).map(f)
      )

    override def mapConcat[C](f: B => Iterable[C])(implicit In: Tag[In] @uncheckedVariance): Query[In, C] =
      new Query.Accumulate[In, Out, S, C](
        source,
        initialState,
        modifyState,
        getResults(_).flatMap(f)
      )

    override def collect[C](pf: PartialFunction[B, C]): Query[In, C] =
      new Query.Accumulate[In, Out, S, C](
        source,
        initialState,
        modifyState,
        getResults(_).collect(pf)
      )

    override def where(p: Predicate[B]): Query[In, B] =
      new Query.Accumulate[In, Out, S, B](
        source,
        initialState,
        modifyState,
        getResults(_).filter(p)
      )
  }

  final class SortByQuery[In, Out, By](
    private[scalaql] val source:         Query[In, Out],
    private[scalaql] val sortBy:         SortBy[Out, By]
  )(private[scalaql] implicit val order: Order[By])
      extends Query[In, Out] {

    override def toString: String = s"$source -> SORT_BY"
  }

  final class AggregateQuery[In, Out0, G, Out1, Out2](
    private[scalaql] val source: Query[In, Out0],
    private[scalaql] val group:  GroupBy[Out0, G],
    private[scalaql] val agg:    Aggregate[G, Out0, Out1],
    private[scalaql] val tupled: TupleFlatten.Aux[Out1, Out2])
      extends Query[In, Out2] {

    override def toString: String = s"$source -> AGGREGATE"
  }

  sealed trait GroupByQuery[-In, +Out, +G] {

    def aggregate[B](
      f:               Aggregate[G, Out, B] @uncheckedVariance
    )(implicit tupled: TupleFlatten[B]
    ): Query[In, tupled.Out]
  }

  final class GroupByQueryImpl[In, Out, G](
    private[scalaql] val source: Query[In, Out],
    private[scalaql] val group:  GroupBy[Out, G])
      extends GroupByQuery[In, Out, G] {

    override def aggregate[B](f: Aggregate[G, Out, B])(implicit tupled: TupleFlatten[B]): Query[In, tupled.Out] =
      new AggregateQuery[In, Out, G, B, tupled.Out](source, group, f, tupled)
  }

  final class InnerJoinPartiallyApplied[In, Out, Out2](
    left:     Query[In, Out],
    right:    Query[In, Out2],
    joinType: InnerJoinType) {

    def on(f: (Out, Out2) => Boolean): Query[In, (Out, Out2)] =
      new InnerJoinedQuery[In, Out, Out2](left, right, joinType, f)
  }

  private[scalaql] sealed abstract class InnerJoinType(override val toString: String)
  private[scalaql] case object InnerJoin extends InnerJoinType("INNER")
  private[scalaql] case object CrossJoin extends InnerJoinType("CROSS")

  sealed trait JoinedQuery[In, Out, Out2, Res] extends Query[In, Res] {
    private[scalaql] val left: Query[In, Out]
    private[scalaql] val right: Query[In, Out2]
  }

  final class InnerJoinedQuery[In, Out, Out2](
    private[scalaql] val left:     Query[In, Out],
    private[scalaql] val right:    Query[In, Out2],
    private[scalaql] val joinType: InnerJoinType,
    private[scalaql] val on:       (Out, Out2) => Boolean)
      extends JoinedQuery[In, Out, Out2, (Out, Out2)] {

    override def toString: String = s"(($left) $joinType JOIN ($right))"

  }

  final class LeftJoinPartiallyApplied[In, Out, Out2](left: Query[In, Out], right: Query[In, Out2]) {

    def on(f: (Out, Out2) => Boolean): Query[In, (Out, Option[Out2])] =
      new LeftJoinedQuery[In, Out, Out2](left, right, f)
  }

  final class LeftJoinedQuery[In, Out, Out2](
    private[scalaql] val left:  Query[In, Out],
    private[scalaql] val right: Query[In, Out2],
    private[scalaql] val on:    (Out, Out2) => Boolean)
      extends JoinedQuery[In, Out, Out2, (Out, Option[Out2])] {

    override def toString: String = s"(($left) LEFT JOIN ($right))"

  }

  final class UnionQuery[In, Out](private[scalaql] val left: Query[In, Out], private[scalaql] val right: Query[In, Out])
      extends Query[In, Out] {

    override def where(p: Predicate[Out]): Query[In, Out] =
      new UnionQuery[In, Out](left.where(p), right.where(p))

    override def map[B](f: Out => B): Query[In, B] =
      new UnionQuery[In, B](left.map(f), right.map(f))

    override def collect[B](pf: PartialFunction[Out, B]): Query[In, B] =
      new UnionQuery[In, B](left.collect(pf), right.collect(pf))

    override def union[In2 <: In, Out0 >: Out](that: Query[In2, Out0]): Query[In2, Out0] =
      new UnionQuery[In2, Out0](left, right union that)

    override def whereSubQuery[In2 <: In](p: Out => QueryResult[In2, Boolean]): Query[In2, Out] =
      new UnionQuery[In2, Out](left.whereSubQuery(p), right.whereSubQuery(p))

    override def toString: String = s"(($left) UNION ($right))"
  }

  final class AndThenQuery[In0, OutA, OutB](
    private[scalaql] val left:    Query[In0, OutA],
    private[scalaql] val right:   Query[From[OutA], OutB],
    private[scalaql] val outATag: LightTypeTag)
      extends Query[In0, OutB] {

    override def toString: String = s"(($left) AND_THEN ($right))"

    override def andThen[Out0 >: OutB: Tag, Out2](that: Query[From[Out0], Out2]): Query[In0, Out2] =
      new AndThenQuery[In0, OutA, Out2](
        left,
        right >>> that,
        outATag
      )

    override def map[B](f: OutB => B): Query[In0, B] =
      new AndThenQuery[In0, OutA, B](
        left,
        right.map(f),
        outATag
      )

    override def where(p: Predicate[OutB]): Query[In0, OutB] =
      new AndThenQuery[In0, OutA, OutB](
        left,
        right.where(p),
        outATag
      )

    override def collect[B](pf: PartialFunction[OutB, B]): Query[In0, B] =
      new AndThenQuery[In0, OutA, B](
        left,
        right.collect(pf),
        outATag
      )
  }
}
