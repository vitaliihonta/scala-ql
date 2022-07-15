package scalaql.syntax

import scalaql.Query
import scalaql.QueryResult

final class MapQuerySyntax[In, K, V](private val self: Query[In, (K, V)]) extends AnyVal {

  /**
   * Collects `this` query output values into a scala `Map`
   *
   * @return a `QueryResult` producing a `Map`
   * */
  def toMap: QueryResult[In, Map[K, V]] =
    new QueryResult.CollectMap(self)

}
