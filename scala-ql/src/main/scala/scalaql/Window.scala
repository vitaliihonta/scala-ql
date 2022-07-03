package scalaql

import izumi.reflect.macrortti.LightTypeTag
import scala.util.hashing.MurmurHash3

class Window[A] private (
  partitions:                         List[A => Any],
  private[scalaql] val partitionTags: List[LightTypeTag],
  orders:                             List[(A => Any, Ordering[Any])],
  private[scalaql] val orderTags:     List[LightTypeTag]) {

  private[scalaql] def getPartitionKey(value: A): Int =
    MurmurHash3.orderedHash(
      partitions.map(_.apply(value)).reverse
    )

  def orderBy[B: Tag: Ordering](f: A => B): Window[A] =
    new Window[A](
      partitions,
      partitionTags,
      makeOrder(f) :: orders,
      Tag[B].tag :: orderTags
    )

  def orderBy[B: Tag: Ordering, C: Tag: Ordering](f1: A => B, f2: A => C): Window[A] =
    new Window[A](
      partitions,
      partitionTags,
      makeOrder(f2) :: makeOrder(f1) :: orders,
      Tag[C].tag :: Tag[B].tag :: orderTags
    )

  def orderBy[B: Tag: Ordering, C: Tag: Ordering, D: Tag: Ordering](f1: A => B, f2: A => C, f3: A => D): Window[A] =
    new Window[A](
      partitions,
      partitionTags,
      makeOrder(f3) :: makeOrder(f2) :: makeOrder(f1) :: orders,
      Tag[D].tag :: Tag[C].tag :: Tag[B].tag :: orderTags
    )

  def orderBy[B: Tag: Ordering, C: Tag: Ordering, D: Tag: Ordering, E: Tag: Ordering](
    f1: A => B,
    f2: A => C,
    f3: A => D,
    f4: A => E
  ): Window[A] =
    new Window[A](
      partitions,
      partitionTags,
      makeOrder(f4) :: makeOrder(f3) :: makeOrder(f2) :: makeOrder(f1) :: orders,
      Tag[E].tag :: Tag[D].tag :: Tag[C].tag :: Tag[B].tag :: orderTags
    )

  private def makeOrder[B: Ordering](f: A => B): (A => Any, Ordering[Any]) =
    (f, Ordering[B].asInstanceOf[Ordering[Any]])

  private[scalaql] def ordering: Ordering[A] = new Window.ChainedOrdering[A](orders.reverse)
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
        orders = Nil,
        orderTags = Nil
      )

    def partitionBy[B: Tag, C: Tag](f1: A => B, f2: A => C): Window[A] =
      new Window[A](
        f2 :: f1 :: partitions,
        Tag[C].tag :: Tag[B].tag :: partitionTags,
        orders = Nil,
        orderTags = Nil
      )

    def partitionBy[B: Tag, C: Tag, D: Tag](f1: A => B, f2: A => C, f3: A => D): Window[A] =
      new Window[A](
        f3 :: f2 :: f1 :: partitions,
        Tag[D].tag :: Tag[C].tag :: Tag[B].tag :: partitionTags,
        orders = Nil,
        orderTags = Nil
      )

    def partitionBy[B: Tag, C: Tag, D: Tag, E: Tag](f1: A => B, f2: A => C, f3: A => D, f4: A => E): Window[A] =
      new Window[A](
        f4 :: f3 :: f2 :: f1 :: partitions,
        Tag[E].tag :: Tag[D].tag :: Tag[C].tag :: Tag[B].tag :: partitionTags,
        orders = Nil,
        orderTags = Nil
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
