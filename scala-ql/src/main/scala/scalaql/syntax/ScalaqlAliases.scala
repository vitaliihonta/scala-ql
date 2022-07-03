package scalaql.syntax

import scalaql.{Aggregation, Ranking, QueryExpressionBuilder, forbiddenInheritance}
import scala.annotation.unchecked.uncheckedVariance

@forbiddenInheritance
trait ScalaqlAliases {
  final type Predicate[-A] = A => Boolean

  final type OrderBy[-A, +B] = A => B

  final type GroupBy[-A, +B] = A => B

  final type Aggregate[-G, -A, +B] =
    (G, QueryExpressionBuilder[A] @uncheckedVariance) => Aggregation.Of[A, B @uncheckedVariance]

  final type Tag[A] = izumi.reflect.Tag[A]
  final val Tag = izumi.reflect.Tag
}
