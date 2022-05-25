package scalaql.syntax

import scalaql.Query
import scalaql.QueryResult
import scalaql.internal.FunctionK

final class BasicQuerySyntax[In, Out](private val self: Query[In, Out]) extends AnyVal {

  final def toList: QueryResult[In, List[Out]] =
    new QueryResult.Collect(self, FunctionK.listBufferToList)

  final def distinct: QueryResult[In, Set[Out]] =
    new QueryResult.Collect(self, FunctionK.listBufferToSet)

  final def find(p: Out => Boolean): QueryResult[In, Option[Out]] =
    new QueryResult.Find(self, p)

  final def exists(p: Out => Boolean): QueryResult[In, Boolean] =
    find(p).map(_.nonEmpty)
}
