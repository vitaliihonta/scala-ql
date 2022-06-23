package scalaql.syntax

import scalaql.describe.{Describe, DescribeContext, DescribeVisitorImpl, RowDescription}
import scalaql.Query

class DescribeSyntax[In, Out: Describe](self: Query[In, Out]) {

  def describe(): Query[In, RowDescription] =
    self.accumulate(
      DescribeVisitorImpl.empty
    ) { (visitor, value) =>
      Describe[Out].apply(value, visitor)(DescribeContext.initial)
      visitor
    }(_.getStats)
}
