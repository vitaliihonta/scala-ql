package scalaql

import scala.language.experimental.macros
import izumi.reflect.macrortti.LightTypeTag
import scala.util.hashing.MurmurHash3
//import scalaql.internal.WindowOrderByMacro

class Window[A] @internalApi() (
  @internalApi() val __scalaql_window_partitions:    List[A => Any],
  @internalApi() val __scalaql_window_partitionTags: List[LightTypeTag],
  @internalApi() val __scalaql_window_orders:        List[(A => Any, Ordering[Any])],
  @internalApi() val __scalaql_window_orderTags:     List[LightTypeTag]) {

  private[scalaql] def getPartitionKey(value: A): Int =
    MurmurHash3.orderedHash(
      __scalaql_window_partitions.map(_.apply(value)).reverse
    )

  def orderBy[B: Tag: Ordering](f: A => B): Window[A] =
    new Window[A](
      __scalaql_window_partitions,
      __scalaql_window_partitionTags,
      makeOrder(f) :: __scalaql_window_orders,
      Tag[B].tag :: __scalaql_window_orderTags
    )

//  def orderBy[B](f: A => B)(implicit ordering: Ordering[B]): Window[A] =
//  macro WindowOrderByMacro.orderBy[A, B]

  // TODO: implement with macro
  def orderBy[B: Tag: Ordering, C: Tag: Ordering](f1: A => B, f2: A => C): Window[A] =
    new Window[A](
      __scalaql_window_partitions,
      __scalaql_window_partitionTags,
      makeOrder(f2) :: makeOrder(f1) :: __scalaql_window_orders,
      Tag[C].tag :: Tag[B].tag :: __scalaql_window_orderTags
    )

  def orderBy[B: Tag: Ordering, C: Tag: Ordering, D: Tag: Ordering](f1: A => B, f2: A => C, f3: A => D): Window[A] =
    new Window[A](
      __scalaql_window_partitions,
      __scalaql_window_partitionTags,
      makeOrder(f3) :: makeOrder(f2) :: makeOrder(f1) :: __scalaql_window_orders,
      Tag[D].tag :: Tag[C].tag :: Tag[B].tag :: __scalaql_window_orderTags
    )

  def orderBy[B: Tag: Ordering, C: Tag: Ordering, D: Tag: Ordering, E: Tag: Ordering](
    f1: A => B,
    f2: A => C,
    f3: A => D,
    f4: A => E
  ): Window[A] =
    new Window[A](
      __scalaql_window_partitions,
      __scalaql_window_partitionTags,
      makeOrder(f4) :: makeOrder(f3) :: makeOrder(f2) :: makeOrder(f1) :: __scalaql_window_orders,
      Tag[E].tag :: Tag[D].tag :: Tag[C].tag :: Tag[B].tag :: __scalaql_window_orderTags
    )

  private def makeOrder[B: Ordering](f: A => B): (A => Any, Ordering[Any]) =
    (f, Ordering[B].asInstanceOf[Ordering[Any]])

  private[scalaql] def ordering: Ordering[A] = new Window.ChainedOrdering[A](__scalaql_window_orders.reverse)
}

object Window {
  private val initial: Window.Builder[Any] = new Window.Builder[Any](Nil, Nil)

  def apply[A]: Window.Builder[A] = initial.asInstanceOf[Window.Builder[A]]

  // This is to ensure that there are no empty partition
  final class Builder[A](
    partitions:                         List[A => Any],
    private[scalaql] val partitionTags: List[LightTypeTag]) {

    def partitionBy[B: Tag](f: A => B): Window[A] =
      new Window[A](
        f :: partitions,
        Tag[B].tag :: partitionTags,
        __scalaql_window_orders = Nil,
        __scalaql_window_orderTags = Nil
      )

    def partitionBy[B: Tag, C: Tag](f1: A => B, f2: A => C): Window[A] =
      new Window[A](
        f2 :: f1 :: partitions,
        Tag[C].tag :: Tag[B].tag :: partitionTags,
        __scalaql_window_orders = Nil,
        __scalaql_window_orderTags = Nil
      )

    def partitionBy[B: Tag, C: Tag, D: Tag](f1: A => B, f2: A => C, f3: A => D): Window[A] =
      new Window[A](
        f3 :: f2 :: f1 :: partitions,
        Tag[D].tag :: Tag[C].tag :: Tag[B].tag :: partitionTags,
        __scalaql_window_orders = Nil,
        __scalaql_window_orderTags = Nil
      )

    def partitionBy[B: Tag, C: Tag, D: Tag, E: Tag](f1: A => B, f2: A => C, f3: A => D, f4: A => E): Window[A] =
      new Window[A](
        f4 :: f3 :: f2 :: f1 :: partitions,
        Tag[E].tag :: Tag[D].tag :: Tag[C].tag :: Tag[B].tag :: partitionTags,
        __scalaql_window_orders = Nil,
        __scalaql_window_orderTags = Nil
      )
  }

  private[scalaql] class ChainedOrdering[A](orders: List[(A => Any, Ordering[Any])]) extends Ordering[A] {
    override def compare(x: A, y: A): Int = {
      val iter   = orders.iterator
      var result = 0
      while (iter.hasNext && result == 0) {
        val (f, ordering) = iter.next()
        result = ordering.compare(f(x), f(y))
      }
      result
    }
  }
}
