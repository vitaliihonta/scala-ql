package scalaql.syntax

import scalaql.Aggregation.Of
import scalaql.{Aggregation, QueryExpressionBuilder}

trait ReportPartiallyApplied2[A, B, C, U1] {
  def combine[U2](agg2: (B, QueryExpressionBuilder[U1]) => Aggregation.Of[U1, U2]): Aggregation.Of[A, List[U2]]
}

trait ReportPartiallyApplied2Syntax[A] { this: QueryExpressionBuilder.Impl[A] =>
  override def report[B, C, U1](
    group1: A => B,
    group2: A => C
  )(merge:  (B, C, QueryExpressionBuilder[A]) => Of[A, U1]
  ): ReportPartiallyApplied2[A, B, C, U1] = new Impl[A, B, C, U1](group1, group2, merge)

  private[scalaql] class Impl[Out, B, C, U1](
    group1: Out => B,
    group2: Out => C,
    agg1:   (B, C, QueryExpressionBuilder[Out]) => Aggregation.Of[Out, U1])
      extends ReportPartiallyApplied2[Out, B, C, U1] {

    override def combine[U2](
      agg2: (B, QueryExpressionBuilder[U1]) => Aggregation.Of[U1, U2]
    ): Aggregation.Of[Out, List[U2]] =
      new Aggregation.Report2[Out, B, C, U1, U2](
        QueryExpressionBuilder.create[Out],
        QueryExpressionBuilder.create[U1]
      )(
        group1,
        group2,
        agg1,
        agg2
      )
  }
}
