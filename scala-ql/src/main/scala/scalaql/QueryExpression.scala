package scalaql

import scalaql.internal.{AggregationFunctions, RankingFunctions}
import scalaql.utils.TupleFlatten
import scala.annotation.unchecked.uncheckedVariance

sealed trait QueryExpression[-A] extends Serializable {
  type Out

  def processWindow(
    order:            Ordering[A] @uncheckedVariance,
    values:           Iterable[A]
  )(implicit flatten: TupleFlatten[(A, Out)] @uncheckedVariance
  ): Iterable[flatten.Out]
}

object QueryExpression {
  type Of[-A, +Out0] = QueryExpression[A] { type Out = Out0 @uncheckedVariance }
}

trait Aggregation[-A] extends QueryExpression[A] { self =>

  def apply(values: Iterable[A]): Out

  def contramap[A0](f: A0 => A): Aggregation.Of[A0, Out] =
    new Aggregation.Contramapped[A0, A, Out](self, f)

  def map[B](f: Out => B): Aggregation.Of[A, B] =
    new Aggregation.Mapped[A, Out, B](self, f)

  def &&[A0 <: A](
    that:            Aggregation[A0]
  )(implicit tupled: TupleFlatten[(Out, that.Out)]
  ): Aggregation.Of[A0, tupled.Out] =
    new Aggregation.Chained[A0, Out, that.Out, tupled.Out](self, that)(tupled)

  override final def processWindow(
    order:            Ordering[A] @uncheckedVariance,
    values:           Iterable[A]
  )(implicit flatten: TupleFlatten[(A, Out)] @uncheckedVariance
  ): Iterable[flatten.Out] = {
    val result = self(values)
    values.map(v => flatten(v -> result))
  }
}

object Aggregation extends AggregationFunctions {
  final type Of[-A, +Out0] = Aggregation[A] { type Out = Out0 @uncheckedVariance }
}

trait Ranking[A] extends QueryExpression[A] { self =>

  def apply(order: Ordering[A], values: Iterable[A]): Iterable[(A, Out)]

//  def contramap[A0](f: A0 => A): Ranking.Of[A0, Ranked] =
//    new Ranking.Contramapped[A, A0, Ranked](self, f)

  def map[B](f: Out => B): Ranking.Of[A, B] =
    new Ranking.Mapped[A, Out, B](self, f)

  override def processWindow(
    order:            Ordering[A] @uncheckedVariance,
    values:           Iterable[A]
  )(implicit flatten: TupleFlatten[(A, Out)]
  ): Iterable[flatten.Out] =
    self.apply(order, values).map(flatten(_))
}

object Ranking extends RankingFunctions {
  final type Of[A, Out0] = Ranking[A] { type Out = Out0 }
}
