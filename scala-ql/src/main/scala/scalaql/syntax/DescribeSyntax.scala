package scalaql.syntax

import scalaql.describe.{Describe, DescribeConfig, DescribeContext, DescribeVisitorImpl, RowDescription}
import scalaql.Query

import java.math.MathContext

final class DescribeSyntax[In, Out: Describe](self: Query[In, Out]) extends Serializable {

  /**
   * Describes the given query output by providing useful statistics,
   * such as average values, means, percentiles for each column.
   *
   * Example:
   * {{{
   *   select[Student]
   *     .describe()
   *     .show(truncate=false)
   *     .run(
   *       from(students)
   *     )
   *
   *   // +--------------+-----+-----+---------+----------+------------+------------+------------+----------+------+
   *   // |field         |count|mean |std      |min       |percentile25|percentile75|percentile90|max       |unique|
   *   // +--------------+-----+-----+---------+----------+------------+------------+------------+----------+------+
   *   // |birthDay      |5    |null |null     |1977-10-01|null        |null        |null        |1980-07-31|[]    |
   *   // |grade         |5    |85.22|10.88474 |66.2      |85.1        |90.1        |99.6        |99.6      |[]    |
   *   // |name          |5    |null |null     |null      |null        |null        |null        |null      |[]    |
   *   // |specialization|5    |null |null     |null      |null        |null        |null        |null      |[]    |
   *   // |age           |5    |18.0 |0.6324555|17.0      |18.0        |18.0        |19.0        |19.0      |[]    |
   *   // |faculty       |5    |null |null     |null      |null        |null        |null        |null      |[]    |
   *   // +--------------+-----+-----+---------+----------+------------+------------+------------+----------+------+
   *   //
   * }}}
   *
   * Based on original idea of Pandas authors.
   * @see [[https://pandas.pydata.org/docs/reference/api/pandas.DataFrame.describe.html DataFrame.describe]]
   *
   * @param precision decimal precision to use for computing statistics
   * @param unique collect unique values or not
   * @return a `Query` producing statistics of each output value of this query.
   * */
  def describe(precision: MathContext = MathContext.DECIMAL32, unique: Boolean = false): Query[In, RowDescription] =
    self.accumulate(
      DescribeVisitorImpl.empty(DescribeConfig(precision, unique))
    ) { (visitor, value) =>
      Describe[Out].apply(value, visitor)(DescribeContext.initial)
      visitor
    }(_.getStats)
}
