package scalaql.syntax

import scalaql.*
import scalaql.internal.FunctionK

final class BasicQuerySyntax[In, Out](private val self: Query[In, Out]) extends AnyVal {

  /**
   * Collects `this` query output values into a scala `List`
   *
   * @return a `QueryResult` producing a `List`
   * */
  def toList: QueryResult[In, List[Out]] =
    new QueryResult.Collect(self, FunctionK.listBufferToList)

  /**
   * Collects `this` query distinct output values into a scala `Set`
   *
   * @return a `QueryResult` producing a `Set`
   * */
  def distinct: QueryResult[In, Set[Out]] =
    new QueryResult.Collect(self, FunctionK.listBufferToSet)

  /**
   * Finds an output value of `this` query
   * for which the given predicate holds (if exists)
   *
   * @param p the predicate
   * @return matched value, if found
   * */
  def find(p: Predicate[Out]): QueryResult[In, Option[Out]] =
    new QueryResult.Find(self, p)

  /**
   * Checks whenever any output value of `this` query exist
   * for which the given predicate holds
   *
   * @param p the predicate
   * @return true if matched value exists, otherwise false
   * */
  def exists(p: Predicate[Out]): QueryResult[In, Boolean] =
    find(p).map(_.nonEmpty)

  /**
   * Executes given side effecting function for each of `this` query
   * output value.
   *
   * @param f the side effect
   * @return the specified side effect executed
   * */
  def foreach(f: => (Out => Unit)): QueryResult[In, Unit] =
    new QueryResult.Foreach(self, () => f)

  /**
   * Collects `this` query output values into a scala `Map`
   * with the key computed based on the output value.
   *
   * @tparam K the key value
   * @param f the key extractor
   * @return a `QueryResult` producing a `Map`
   * */
  def toMapBy[K: Tag](f: Out => K)(implicit outTag: Tag[Out]): QueryResult[In, Map[K, Out]] =
    new QueryResult.CollectMap(self.map(out => f(out) -> out))
}
