package scalaql.syntax

import scalaql._

final class MapQuerySyntax[In, K, V](private val self: Query[In, (K, V)]) extends AnyVal {

  final def toMap: QueryResult[In, Map[K, V]] =
    new QueryResult.CollectMap(self)

}
