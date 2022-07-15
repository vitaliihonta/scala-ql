package scalaql.syntax

import scalaql.QueryResult
import scalaql.ToFrom
import scalaql.internal.QueryResultRunner

final class RunSyntax[In, Out](private val self: QueryResult[In, Out]) extends AnyVal {

  /**
   * Runs this `QueryResult`, evaluating all of the upstream queries.
   *
   * Example:
   * {{{
   * val peopleList =
   *   select[Person]
   *     .toList
   *     .run(from(people))
   * }}}
   *
   * @param in query input
   * @return the query result
   * */
  def run(in: In)(implicit toFrom: ToFrom[In]): Out = QueryResultRunner.runImpl(self)(in)
}

final class RunSyntaxAny[Out](private val self: QueryResult[Any, Out]) extends AnyVal {

  /** @see [[RunSyntax.run]] */
  def run: Out = QueryResultRunner.runImpl[Any, Out](self)(())
}
