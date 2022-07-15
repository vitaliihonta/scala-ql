package scalaql

import scalaql.internal.FunctionK
import scala.collection.mutable.ListBuffer

/**
 * Represents the result of executing a query.
 * It's the result type of the following methods:
 *
 * 1. Collecting query results into a list
 * {{{
 *   val query: Query[From[Person], PeopleStatistics]] = ???
 *
 *   val result = query.toList
 * }}}
 *
 * 2. Printing query results into the console
 * {{{
 *   val query: Query[From[Person], PeopleStatistics]] = ???
 *
 *   val result = query.show(truncate = false)
 * }}}
 *
 * 3. Creating predicates that could be used in other queries
 * {{{
 *   def hasAdults(faculty: Faculty) =
 *     select[Student]
 *       .where(_.faculty == faculty.name)
 *       .exists(_.age >= 18)
 *
 *   select[Faculty].whereSubQuery(hasAdults)
 * }}}
 *
 * @tparam In input type
 * @tparam Out query result type
 * */
sealed trait QueryResult[-In, +Out] extends Serializable {

  /**
   * Transforms this `QueryResult` by applying an arbitrary function.
   *
   * Example:
   * {{{
   *   val result = select[Person].toList
   *   val result2 = result.map(_.size)
   * }}}
   *
   * @tparam B new query result type
   * @param f transformation function
   * @return transformed query result
   * */
  def map[B](f: Out => B): QueryResult[In, B] =
    new QueryResult.Mapped[In, Out, B](this, f)

  /**
   * Transforms this `QueryResult` by applying a function producing other `QueryResult`
   *
   * Example:
   * {{{
   *   val peopleQuery: Query[From[Person], Person] = ???
   *   val companiesQuery: Query[From[Company], Company] = ???
   *
   *   val result = for {
   *     people <- peopleQuery.toList
   *     companies <- companiesQuery.toList
   *   } yield (people, companies)
   * }}}
   *
   * @tparam B new query result type
   * @param f transformation function
   * @return transformed query result
   * */
  def flatMap[In2 <: In, B](f: Out => QueryResult[In2, B]): QueryResult[In2, B] =
    new QueryResult.FlatMapped[In2, Out, B](this, f)
}

object QueryResult {

  /**
   * Allows to wrap an existing value into a `QueryResult`
   *
   * @tparam A input type
   * @param value existing value
   * @return a `QueryResults` which produces given value for any possible input.
   * */
  def const[A](value: A): QueryResult[Any, A] = new Const(value)

  final class Const[Out](private[scalaql] val value: Out) extends QueryResult[Any, Out]

  final class Collect[Coll[_], In, Out](
    private[scalaql] val query:     Query[In, Out],
    private[scalaql] val mapResult: FunctionK[ListBuffer, Coll])
      extends QueryResult[In, Coll[Out]]

  final class CollectMap[In, K, V](private[scalaql] val query: Query[In, (K, V)]) extends QueryResult[In, Map[K, V]]

  final class Foreach[In, Out](
    private[scalaql] val query:         Query[In, Out],
    private[scalaql] val createForeach: () => (Out) => Unit)
      extends QueryResult[In, Unit]

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
