package scalaql.syntax

import scalaql.describe.{Describe, DescribeConfig, DescribeContext, DescribeVisitorImpl, RowDescription}
import scalaql.Query

import java.math.MathContext

final class DescribeSyntax[In, Out: Describe](self: Query[In, Out]) extends Serializable {

  def describe(precision: MathContext = MathContext.DECIMAL32, unique: Boolean = false): Query[In, RowDescription] =
    self.accumulate(
      DescribeVisitorImpl.empty(DescribeConfig(precision, unique))
    ) { (visitor, value) =>
      Describe[Out].apply(value, visitor)(DescribeContext.initial)
      visitor
    }(_.getStats)
}
