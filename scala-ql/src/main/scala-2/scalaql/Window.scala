package scalaql

import izumi.reflect.macrortti.LightTypeTag
import scalaql.internal.{ChainedOrdering, WindowOrderByMacro}

import scala.language.experimental.macros
import scala.util.hashing.MurmurHash3

object Window {
  private val initial: WindowBuilder[Any] = new WindowBuilder[Any](Nil, Nil)

  def apply[A]: WindowBuilder[A] = initial.asInstanceOf[WindowBuilder[A]]
}

/**
 * Description of a window function.
 * Used inside an `over` clause when defining a windowed query:
 * `.over(_.partitionBy(_.country))`
 *
 * Example:
 * {{{
 *   select[Person]
 *     .window(_.rowNumber)
 *     .over(_.partitionBy(_.country))
 * }}}
 *
 * @tparam A the input type of `this` window function
 * */
class Window[A] @internalApi() (
  @internalApi() val __scalaql_window_partitions:    List[A => Any],
  @internalApi() val __scalaql_window_partitionTags: List[LightTypeTag],
  @internalApi() val __scalaql_window_orders:        List[(A => Any, Ordering[Any])],
  @internalApi() val __scalaql_window_orderTags:     List[LightTypeTag]) {

  private[scalaql] def getPartitionKey(value: A): Int =
    MurmurHash3.orderedHash(
      __scalaql_window_partitions.map(_.apply(value)).reverse
    )

  /**
   * Specifies ordering for `this` window function input values.
   *
   * Example:
   * {{{
   *   select[Person]
   *     .window(_.rowNumber)
   *     .over(
   *       _.partitionBy(_.country)
   *         .orderBy(_.age)
   *     )
   * }}}
   *
   * @tparam B ordering key type
   * @param f get ordering key
   * @param orderingB implicit ordering for the key
   * @return `this` window function with ordering
   * */
  def orderBy[B](f: A => B)(implicit orderingB: Ordering[B]): Window[A] =
    macro WindowOrderByMacro.orderBy1[A, B]

  /**
   * Specifies ordering for `this` window function input values.
   *
   * Example:
   * {{{
   *   select[Person]
   *     .window(_.rowNumber)
   *     .over(
   *       _.partitionBy(_.country)
   *         .orderBy(_.name, _.age.desc)
   *     )
   * }}}
   *
   * @tparam B first ordering key type
   * @tparam C second ordering key type
   * @param f1 get the first ordering key
   * @param f2 get the second ordering key
   * @param orderingB implicit ordering for the first key
   * @param orderingC implicit ordering for the second key
   * @return `this` window function with ordering
   * */
  def orderBy[B, C](f1: A => B, f2: A => C)(implicit orderingB: Ordering[B], orderingC: Ordering[C]): Window[A] =
    macro WindowOrderByMacro.orderBy2[A, B, C]

  /**
   * Specifies ordering for `this` window function input values.
   *
   * Example:
   * {{{
   *   select[Person]
   *     .window(_.rowNumber)
   *     .over(
   *       _.partitionBy(_.country)
   *         .orderBy(_.name, _.age.desc, _.salary)
   *     )
   * }}}
   *
   * @tparam B first ordering key type
   * @tparam C second ordering key type
   * @tparam D third ordering key type
   * @param f1 get the first ordering key
   * @param f2 get the second ordering key
   * @param f3 get the third ordering key
   * @param orderingB implicit ordering for the first key
   * @param orderingC implicit ordering for the second key
   * @param orderingD implicit ordering for the third key
   * @return `this` window function with ordering
   * */
  def orderBy[B, C, D](
    f1:                 A => B,
    f2:                 A => C,
    f3:                 A => D
  )(implicit orderingB: Ordering[B],
    orderingC:          Ordering[C],
    orderingD:          Ordering[D]
  ): Window[A] =
    macro WindowOrderByMacro.orderBy3[A, B, C, D]

  private[scalaql] def ordering: Ordering[A] = new ChainedOrdering[A](__scalaql_window_orders.reverse)
}
