package scalaql.syntax

import scalaql.Query
import scalaql.QueryResult
import scalaql.internal.FunctionK
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

final class QueryToSyntax[In, Out](private val self: Query[In, Out]) extends AnyVal {

  final def to[Coll[x] <: Iterable[x]](
    implicit cbf: CanBuildFrom[Coll[Out], Out, Coll[Out]]
  ): QueryResult[In, Coll[Out]] = {
    val mapResult = FunctionK.create[ListBuffer, Coll](buffer =>
      (cbf().asInstanceOf[mutable.Builder[Any, Coll[Any]]] ++= buffer).result()
    )
    new QueryResult.Collect(self, mapResult)
  }
}
