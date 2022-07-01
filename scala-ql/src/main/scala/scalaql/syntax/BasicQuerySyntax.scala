package scalaql.syntax

import scalaql.Tag
import scalaql.Query
import scalaql.QueryResult
import scalaql.internal.FunctionK

final class BasicQuerySyntax[In, Out](private val self: Query[In, Out]) extends AnyVal {

  def toList: QueryResult[In, List[Out]] =
    new QueryResult.Collect(self, FunctionK.listBufferToList)

  def distinct: QueryResult[In, Set[Out]] =
    new QueryResult.Collect(self, FunctionK.listBufferToSet)

  def find(p: Out => Boolean): QueryResult[In, Option[Out]] =
    new QueryResult.Find(self, p)

  def exists(p: Out => Boolean): QueryResult[In, Boolean] =
    find(p).map(_.nonEmpty)

  def foreach(f: => (Out => Unit)): QueryResult[In, Unit] =
    new QueryResult.Foreach(self, () => f)

  def toMapBy[K: Tag](f: Out => K)(implicit outTag: Tag[Out]): QueryResult[In, Map[K, Out]] =
    new QueryResult.CollectMap(self.map(out => f(out) -> out))
}
