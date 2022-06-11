package scalaql.syntax

import scalaql.QueryResult
import scalaql.ToFrom
import scalaql.internal.QueryResultRunner

final class RunSyntax[In, Out](private val self: QueryResult[In, Out]) extends AnyVal {
  def run(in: In)(implicit toFrom: ToFrom[In]): Out = QueryResultRunner.runImpl(self)(in)
}

final class RunSyntaxAny[Out](private val self: QueryResult[Any, Out]) extends AnyVal {
  def run: Out = QueryResultRunner.runImpl[Any, Out](self)(())
}
