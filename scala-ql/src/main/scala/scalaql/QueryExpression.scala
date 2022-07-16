package scalaql

import scalaql.internal.{AggregationFunctions, RankingFunctions}
import scalaql.utils.TupleFlatten
import scala.annotation.unchecked.uncheckedVariance

/**
 * Represents a base type for either aggregation function or ranking.
 * */
sealed trait QueryExpression[-A] extends Serializable {

  /**
   * Output value of this expression.
   * */
  type Out

  def processWindow(
    order:            Ordering[A] @uncheckedVariance,
    values:           Iterable[A]
  )(implicit flatten: TupleFlatten[(A, Out)] @uncheckedVariance
  ): Iterable[flatten.Out]
}

object QueryExpression {
  final type Of[-A, +Out0] = QueryExpression[A] { type Out = Out0 @uncheckedVariance }
}

/**
 * Description of an aggregation.
 * It's a return type of aggregation function used inside `groupBy` -> `aggregate` query
 * or inside a window function.
 *
 * For instance, `person.avgBy(_.age.toDouble)` is an `Aggregation.Of[Person, Double]`.
 * 
 * Example:
 * {{{
 *   select[Person]
 *     .groupBy(_.country)
 *     .aggregate((country, person) => person.avgBy(_.age.toDouble))
 * }}}
 * 
 * @tparam A input type of the aggregation function
 * */
trait Aggregation[-A] extends QueryExpression[A] { self =>

  /**
   * Applies `this` aggregation function to a set of values.
   *
   * @param values values to aggregate
   * @return the aggregation result
   * */
  def apply(values: Iterable[A]): Out

  /**
   * Transforms `this` aggregation function input type.
   *
   * @tparam A0 the new input type
   * @param f reverse transformation function
   * @return `this` aggregation function with new input type
   * */
  def contramap[A0](f: A0 => A): Aggregation.Of[A0, Out] =
    new Aggregation.Contramapped[A0, A, Out](self, f)

  /**
   * Transforms `this` aggregation function output type.
   *
   * @tparam B the new output type
   * @param f  transformation function
   * @return `this` aggregation function with transformed output
   * */
  def map[B](f: Out => B): Aggregation.Of[A, B] =
    new Aggregation.Mapped[A, Out, B](self, f)

  /**
   * Chains multiple aggregation functions into a single one.
   * Used to apply multiple aggregations to a single record
   * For instance, `person.avgBy(_.age.toDouble) && person.sumBy(_.salary)`
   * is an `Aggregation.Of[Person, (Double, BigDecimal)]`.
   *
   * Example:
   * {{{
   *   select[Person]
   *     .groupBy(_.country)
   *     .aggregate((country, person) =>
   *       person.avgBy(_.age.toDouble) && person.sumBy(_.salary)
   *     )
   * }}}
   *
   * @param that aggregation function to chain with
   * @return `this` aggregation function chained `that`
   * */
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

/**
 * Description of an ranking function.
 * It's a return type of ranking function used inside a window function.
 *
 * For instance, `person.rowNumber` is an `Ranking.Of[Person, Int]`.
 *
 * Example:
 * {{{
 *   select[Person]
 *     .window(_.rowNumber)
 *     .over(_.partitionBy(_.country))
 * }}}
 *
 * @tparam A input type of the ranking function
 * */
trait Ranking[A] extends QueryExpression[A] { self =>

  /**
   * Applies `this` ranking function to a set of values.
   *
   * @param order ordering for the input type
   * @param values values to aggregate
   * @return the ranking function result
   * */
  def apply(order: Ordering[A], values: Iterable[A]): Iterable[(A, Out)]

  /**
   * Transforms `this` ranking function output type.
   *
   * @tparam B the new output type
   * @param f  transformation function
   * @return `this` ranking function with transformed output
   * */
  def map[B](f: Out => B): Ranking.Of[A, B] =
    new Ranking.Mapped[A, Out, B](self, f)

  override final def processWindow(
    order:            Ordering[A] @uncheckedVariance,
    values:           Iterable[A]
  )(implicit flatten: TupleFlatten[(A, Out)]
  ): Iterable[flatten.Out] =
    self.apply(order, values).map(flatten(_))
}

object Ranking extends RankingFunctions {
  final type Of[A, Out0] = Ranking[A] { type Out = Out0 }
}
