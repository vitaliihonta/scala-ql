package scalaql.syntax

import scalaql.Query
import scalaql.QueryResult
import scalaql.visualization.ShowAsTable
import scalaql.visualization.ShowQueryResult

final class ShowSyntax[In, Out: ShowAsTable](self: Query[In, Out]) extends Serializable {

  def show(numRows: Int = 20, truncate: Boolean = true): QueryResult[In, Unit] =
    new BasicQuerySyntax(self)
      .foreach(ShowQueryResult.sideEffect[Out](numRows, truncate = if (truncate) 20 else 0))
}
