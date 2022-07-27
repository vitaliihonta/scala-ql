package scalaql.syntax

import scalaql.{Aggregation, Ranking, QueryExpressionBuilder, forbiddenInheritance}
import scala.annotation.unchecked.uncheckedVariance

@forbiddenInheritance
trait ScalaqlAliases {
  final type Predicate[-A] = A => Boolean

  final type Tag[A] = izumi.reflect.Tag[A]
  final val Tag = izumi.reflect.Tag
}
