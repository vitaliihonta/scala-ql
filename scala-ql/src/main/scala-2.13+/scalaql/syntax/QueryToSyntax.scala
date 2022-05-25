package scalaql.syntax

import scalaql._
import scalaql.internal.FunctionK
import scala.collection.IterableFactory
import scala.collection.mutable.ListBuffer

final class QueryToSyntax[In, Out](private val self: Query[In, Out]) extends AnyVal {

  def to[Coll[x]](fac: IterableFactory[Coll]): QueryResult[In, Coll[Out]] = {
    val mapResult = FunctionK.create[ListBuffer, Coll](buffer => (fac.newBuilder ++= buffer).result())
    new QueryResult.Collect[Coll, In, Out](self, mapResult)
  }
}
