package scalaql.syntax

import scalaql.Query
import scalaql.QueryResult
import scalaql.visualization.ShowAsTable
import scalaql.visualization.ShowQueryResult

final class ShowSyntax[In, Out](private val self: Query[In, Out]) extends AnyVal {

  /**
   * Pretty-prints this query outputs into a console table.
   *
   * Example:
   * {{{
   * select[Student]
   *   .show(truncate = false)
   *   .run(from(students))
   *
   * // +--------+---+----------+-----+---------------------+----------+
   * // |name    |age|faculty   |grade|specialization       |birthDay  |
   * // +--------+---+----------+-----+---------------------+----------+
   * // |Harry   |19 |Gryffindor|85.1 |getting into troubles|1980-07-31|
   * // |Ron     |18 |Gryffindor|66.2 |eating               |1980-05-01|
   * // |Hermione|18 |Gryffindor|99.6 |learning             |1979-09-17|
   * // |Draco   |18 |Slytherin |85.1 |trolling             |1980-06-05|
   * // +--------+---+----------+-----+---------------------+----------+
   * //
   * }}}
   *
   * @param numRows the maximum number of rows to show
   * @param truncate limits the width of the table
   * @return a `QueryResult` which prints `this` query output into console.
   * */
  def show(numRows: Int = 20, truncate: Boolean = true)(implicit showAsTable: ShowAsTable[Out]): QueryResult[In, Unit] =
    new BasicQuerySyntax(self)
      .foreach(ShowQueryResult.sideEffect[Out](numRows, truncate = if (truncate) 20 else 0))
}
