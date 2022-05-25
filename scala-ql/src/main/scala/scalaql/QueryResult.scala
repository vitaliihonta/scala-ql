package scalaql

import scalaql.internal.FunctionK
import scala.collection.mutable.ListBuffer

sealed trait QueryResult[-In, +Out] {

  def map[B](f: Out => B): QueryResult[In, B] =
    new QueryResult.Mapped[In, Out, B](this, f)

  def flatMap[In2 <: In, B](f: Out => QueryResult[In2, B]): QueryResult[In2, B] =
    new QueryResult.FlatMapped[In2, Out, B](this, f)
}

object QueryResult {

  def const[A](value: A): QueryResult[Any, A] = new Const(value)

  final class Const[Out](private[scalaql] val value: Out) extends QueryResult[Any, Out]

  final class Collect[Coll[_], In, Out](
    private[scalaql] val query:     Query[In, Out],
    private[scalaql] val mapResult: FunctionK[ListBuffer, Coll])
      extends QueryResult[In, Coll[Out]]

  final class CollectMap[In, K, V](private[scalaql] val query: Query[In, (K, V)]) extends QueryResult[In, Map[K, V]]

  final class Find[In, Out](private[scalaql] val query: Query[In, Out], private[scalaql] val predicate: Predicate[Out])
      extends QueryResult[In, Option[Out]]

  final class Mapped[In, Out0, Out1](
    private[scalaql] val base:    QueryResult[In, Out0],
    private[scalaql] val project: Out0 => Out1)
      extends QueryResult[In, Out1] {

    override def map[B](f: Out1 => B): QueryResult[In, B] =
      new Mapped[In, Out0, B](base, project andThen f)
  }

  final class FlatMapped[In, Out0, Out1](
    private[scalaql] val base: QueryResult[In, Out0],
    private[scalaql] val bind: Out0 => QueryResult[In, Out1])
      extends QueryResult[In, Out1] {

    override def map[B](f: Out1 => B): QueryResult[In, B] =
      new FlatMapped[In, Out0, B](base, bind(_).map(f))

    override def flatMap[In2 <: In, B](f: Out1 => QueryResult[In2, B]): QueryResult[In2, B] =
      new FlatMapped[In2, Out0, B](base, bind(_).flatMap(f))
  }
}
