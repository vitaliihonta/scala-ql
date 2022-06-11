package scalaql.syntax

import scalaql.Query
import scalaql.QueryResult

final class MapQuerySyntax[In, K, V](private val self: Query[In, (K, V)]) extends AnyVal {

  def toMap: QueryResult[In, Map[K, V]] =
    new QueryResult.CollectMap(self)

}
