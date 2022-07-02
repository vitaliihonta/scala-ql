package scalaql

import izumi.reflect.macrortti.LightTypeTag
import spire.algebra.Order

class Window[A](
  private[scalaql] val partitionBy:   List[A => Any],
  private[scalaql] val partitionTags: List[LightTypeTag],
  private[scalaql] val orderBy:       List[(A => Any, Order[Any])],
  private[scalaql] val orderTags:     List[LightTypeTag]) {

  def partitionBy[B: Tag](f: A => B): Window[A] =
    new Window[A](
      f :: partitionBy,
      Tag[B].tag :: partitionTags,
      orderBy,
      orderTags
    )

  def orderBy[B: Tag: Order](f: A => B): Window[A] =
    new Window[A](
      partitionBy,
      partitionTags,
      (f, Order[B].asInstanceOf[Order[Any]]) :: orderBy,
      Tag[B].tag :: orderTags
    )
}

object Window {
  def apply[A]: Window[A] = initial.asInstanceOf[Window[A]]

  private val initial: Window[Any] = new Window[Any](Nil, Nil, Nil, Nil)
}
