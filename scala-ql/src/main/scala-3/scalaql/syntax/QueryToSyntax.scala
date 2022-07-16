package scalaql.syntax

import scalaql.Query
import scalaql.QueryResult
import scalaql.internal.FunctionK
import scala.collection.IterableFactory
import scala.collection.mutable.ListBuffer

final class QueryToSyntax[In, Out](private val self: Query[In, Out]) extends AnyVal {

  /**
   * Collects `this` query output values into the specified scala collection.
   *
   * Example:
   * {{{
   *   select[Person].to(Vector)
   * }}}
   *
   * @tparam Coll concrete scala collection type
   * @param fac companion object of desired scala collection.
   * @return `QueryResult` which collects `this` query output values into the specified collection.
   * */
  def to[Coll[x]](fac: IterableFactory[Coll]): QueryResult[In, Coll[Out]] = {
    val mapResult = FunctionK.create[ListBuffer, Coll](buffer => (fac.newBuilder ++= buffer).result())
    new QueryResult.Collect[Coll, In, Out](self, mapResult)
  }
}
