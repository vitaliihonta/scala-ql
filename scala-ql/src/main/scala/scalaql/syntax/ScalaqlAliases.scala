package scalaql.syntax

import scalaql.AggregationView
import scalaql.Aggregation
import scalaql.forbiddenInheritance
import scala.annotation.unchecked.uncheckedVariance

@forbiddenInheritance
trait ScalaqlAliases {
  final type Predicate[-A] = A => Boolean

  final type SortBy[-A, +B] = A => B

  final type GroupBy[-A, +B] = A => B

  final type Aggregate[-G, -A, +B] =
    (G, AggregationView[A] @uncheckedVariance) => Aggregation.Aux[A, B @uncheckedVariance]
}
