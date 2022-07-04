package scalaql

import scala.language.experimental.macros
import izumi.reflect.macrortti.LightTypeTag

import scala.util.hashing.MurmurHash3
import scalaql.internal.{ChainedOrdering, WindowOrderByMacro}

object Window {
  private val initial: WindowBuilder[Any] = new WindowBuilder[Any](Nil, Nil)

  def apply[A]: WindowBuilder[A] = initial.asInstanceOf[WindowBuilder[A]]
}

class Window[A] @internalApi() (
  @internalApi() val __scalaql_window_partitions:    List[A => Any],
  @internalApi() val __scalaql_window_partitionTags: List[LightTypeTag],
  @internalApi() val __scalaql_window_orders:        List[(A => Any, Ordering[Any])],
  @internalApi() val __scalaql_window_orderTags:     List[LightTypeTag]) {

  private[scalaql] def getPartitionKey(value: A): Int =
    MurmurHash3.orderedHash(
      __scalaql_window_partitions.map(_.apply(value)).reverse
    )

  def orderBy[B](f: A => B)(implicit orderingB: Ordering[B]): Window[A] =
    macro WindowOrderByMacro.orderBy1[A, B]

  def orderBy[B, C](f1: A => B, f2: A => C)(implicit orderingB: Ordering[B], orderingC: Ordering[C]): Window[A] =
    macro WindowOrderByMacro.orderBy2[A, B, C]

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
