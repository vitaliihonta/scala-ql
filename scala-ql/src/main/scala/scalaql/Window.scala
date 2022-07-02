package scalaql

import izumi.reflect.macrortti.LightTypeTag
import spire.algebra.Order
import scala.util.hashing.MurmurHash3

class Window[A] private (
  partitions:                         List[A => Any],
  private[scalaql] val partitionTags: List[LightTypeTag],
  orders:                             List[(A => Any, Order[Any])],
  private[scalaql] val orderTags:     List[LightTypeTag]) {

  def partitionBy[B: Tag](f: A => B): Window[A] =
    new Window[A](
      f :: partitions,
      Tag[B].tag :: partitionTags,
      orders,
      orderTags
    )

  def orderBy[B: Tag: Order](f: A => B): Window[A] =
    new Window[A](
      partitions,
      partitionTags,
      (f, Order[B].asInstanceOf[Order[Any]]) :: orders,
      Tag[B].tag :: orderTags
    )

  def getPartitionKey(value: A): Int =
    MurmurHash3.orderedHash(
      partitions.map(_.apply(value))
    )

  def ordering: Ordering[A] = new Window.ChainedOrdering[A](orders.reverse)
}

object Window {
  def apply[A]: Window[A] = initial.asInstanceOf[Window[A]]

  private val initial: Window[Any] = new Window[Any](Nil, Nil, Nil, Nil)

  private[scalaql] class ChainedOrdering[A](orders: List[(A => Any, Order[Any])]) extends Ordering[A] {
    override def compare(x: A, y: A): Int = {
      val result = orders.foldLeft(Option.empty[Int]) {
        case (compared @ Some(_), _) => compared
        case (_, (by, order)) =>
          val comp = order.compare(by(x), by(y))
          if (comp == 0) None else Some(comp)
      }
      result.getOrElse(0)
    }
  }
}
