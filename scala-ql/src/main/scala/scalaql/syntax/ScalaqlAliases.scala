package scalaql.syntax

import scalaql.AggregationView
import scalaql.Aggregation
import scalaql.forbiddenInheritance
import scala.annotation.unchecked.uncheckedVariance

@forbiddenInheritance
trait ScalaqlAliases {
  final type Predicate[-A] = A => Boolean

  final type OrderBy[-A, +B] = A => B

  final type GroupBy[-A, +B] = A => B

  final type Aggregate[-G, -A, +B] =
    (G, AggregationView[A] @uncheckedVariance) => Aggregation.Of[A, B @uncheckedVariance]

  final type Tag[A] = izumi.reflect.Tag[A]
  final val Tag = izumi.reflect.Tag
}
