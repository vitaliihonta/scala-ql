package scalaql

import izumi.reflect.Tag
import izumi.reflect.macrortti.LightTypeTag
import scalaql.utils.TupleFlatten
import spire.algebra.Order

import scala.annotation.unchecked.uncheckedVariance

sealed trait Query[-In, +Out] {

  def map[B](f: Out => B): Query[In, B] =
    new Query.MapQuery[In, Out, B](this, f)

  def mapFilter[B](f: Out => Option[B]): Query[In, B] =
    new Query.MapFilterQuery[In, Out, B](this, f)

  def collect[B](pf: PartialFunction[Out, B]): Query[In, B] =
    mapFilter(pf.lift)

  def where(p: Predicate[Out]): Query[In, Out] =
    mapFilter(x => Some(x).filter(p))

  def withFilter(p: Predicate[Out]): Query[In, Out] = where(p)

  def whereNot(p: Predicate[Out]): Query[In, Out] =
    where(!p(_))

  def mapConcat[B](f: Out => Iterable[B]): Query[In, B] =
    flatMap(out => new Query.Const[B](f(out)))

  def flatMap[In2 <: In, B](f: Out => Query[In2, B]): Query[In2, B] =
    new Query.FlatMapQuery[In2, Out, B](this, f)

  def whereSubQuery[In2 <: In](p: Out => QueryResult[In2, Boolean]): Query[In2, Out] =
    new Query.WhereSubQuery[In2, Out](this, p)

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
    override def toString: String = "FROM"
  }

  final class MapQuery[In, Out0, Out1](
    private[scalaql] val source:  Query[In, Out0],
    private[scalaql] val project: Out0 => Out1)
      extends Query[In, Out1] {

    override def map[B](f: Out1 => B): Query[In, B] =
      new MapQuery[In, Out0, B](source, project andThen f)

    override def toString: String = s"$source -> map"
  }

  final class FlatMapQuery[In, Out0, Out1](
    private[scalaql] val source:   Query[In, Out0],
    private[scalaql] val projectM: Out0 => Query[In, Out1])
      extends Query[In, Out1] {

    override def map[B](f: Out1 => B): Query[In, B] =
      new FlatMapQuery[In, Out0, B](source, projectM(_).map(f))

    override def where(p: Predicate[Out1]): Query[In, Out1] =
      new FlatMapQuery[In, Out0, Out1](source, projectM(_).where(p))

    override def toString: String = s"$source -> flatMap"
  }

  final class MapFilterQuery[In, Out, Out1](
    private[scalaql] val source:        Query[In, Out],
    private[scalaql] val mapFilterFunc: Out => Option[Out1])
      extends Query[In, Out1] {

    override def mapFilter[B](g: Out1 => Option[B]): Query[In, B] =
      new MapFilterQuery[In, Out, B](source, mapFilterFunc(_).flatMap(g))

    override def toString: String = s"$source -> mapFilter"
  }

  final class WhereSubQuery[In, Out](
    private[scalaql] val source:    Query[In, Out],
    private[scalaql] val predicate: Out => QueryResult[In, Boolean])
      extends Query[In, Out] {

    override def where(p: Predicate[Out]): Query[In, Out] =
      new WhereSubQuery[In, Out](source, x => predicate(x).map(_ && p(x)))

    override def toString: String = s"$source -> filterM"

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

  final class SortByQuery[In, Out, By](
    private[scalaql] val source:         Query[In, Out],
    private[scalaql] val sortBy:         SortBy[Out, By]
  )(private[scalaql] implicit val order: Order[By])
      extends Query[In, Out] {

    override def toString: String = s"$source -> sortBy"
  }

  final class AggregateQuery[In, Out0, G, Out1, Out2](
    private[scalaql] val source: Query[In, Out0],
    private[scalaql] val group:  GroupBy[Out0, G],
    private[scalaql] val agg:    Aggregate[G, Out0, Out1],
    private[scalaql] val tupled: TupleFlatten.Aux[(G, Out1), Out2])
      extends Query[In, Out2] {

    override def toString: String = s"$source -> aggregate"
  }

  sealed trait GroupByQuery[-In, +Out, +G] {
    def aggregate[B](f: Aggregate[G, Out, B])(implicit tupled: TupleFlatten[(G, B)]): Query[In, tupled.Out]
  }

  final class GroupByQueryImpl[In, Out, G](
    private[scalaql] val source: Query[In, Out],
    private[scalaql] val group:  GroupBy[Out, G])
      extends GroupByQuery[In, Out, G] {

    override def aggregate[B](f: Aggregate[G, Out, B])(implicit tupled: TupleFlatten[(G, B)]): Query[In, tupled.Out] =
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

    override def toString: String = s"($left $joinType JOIN $right)"

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

    override def toString: String = s"($left LEFT JOIN $right)"

  }

  final class UnionQuery[In, Out](private[scalaql] val left: Query[In, Out], private[scalaql] val right: Query[In, Out])
      extends Query[In, Out] {

    override def where(p: Predicate[Out]): Query[In, Out] =
      new UnionQuery[In, Out](left.where(p), right.where(p))

    override def map[B](f: Out => B): Query[In, B] =
      new UnionQuery[In, B](left.map(f), right.map(f))

    override def mapFilter[B](f: Out => Option[B]): Query[In, B] =
      new UnionQuery[In, B](left.mapFilter(f), right.mapFilter(f))

    override def union[In2 <: In, Out0 >: Out](that: Query[In2, Out0]): Query[In2, Out0] =
      new UnionQuery[In2, Out0](left, right union that)

    override def whereSubQuery[In2 <: In](p: Out => QueryResult[In2, Boolean]): Query[In2, Out] =
      new UnionQuery[In2, Out](left.whereSubQuery(p), right.whereSubQuery(p))

    override def toString: String = s"($left UNION $right)"
  }

  final class AndThenQuery[In0, OutA, OutB](
    private[scalaql] val left:    Query[In0, OutA],
    private[scalaql] val right:   Query[From[OutA], OutB],
    private[scalaql] val outATag: LightTypeTag)
      extends Query[In0, OutB] {

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

    override def mapFilter[B](f: OutB => Option[B]): Query[In0, B] =
      new AndThenQuery[In0, OutA, B](
        left,
        right.mapFilter(f),
        outATag
      )
  }
}
