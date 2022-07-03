package scalaql

import izumi.reflect.macrortti.LightTypeTag
import scala.util.hashing.MurmurHash3

class Window[A] private (
  partitions:                         List[A => Any],
  private[scalaql] val partitionTags: List[LightTypeTag],
  orders:                             List[(A => Any, Ordering[Any])],
  private[scalaql] val orderTags:     List[LightTypeTag]) {

  def partitionBy[B: Tag](f: A => B): Window[A] =
    new Window[A](
      f :: partitions,
      Tag[B].tag :: partitionTags,
      orders,
      orderTags
    )

  def orderBy[B: Tag: Ordering](f: A => B): Window[A] =
    new Window[A](
      partitions,
      partitionTags,
      (f, Ordering[B].asInstanceOf[Ordering[Any]]) :: orders,
      Tag[B].tag :: orderTags
    )

  def getPartitionKey(value: A): Int =
    MurmurHash3.orderedHash(
      partitions.map(_.apply(value)).reverse
    )

  def ordering: Ordering[A] = new Window.ChainedOrdering[A](orders.reverse)
}

object Window {
  def apply[A]: Window[A] = initial.asInstanceOf[Window[A]]

  private val initial: Window[Any] = new Window[Any](Nil, Nil, Nil, Nil)

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
