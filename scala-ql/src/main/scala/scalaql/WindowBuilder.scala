package scalaql

import izumi.reflect.macrortti.LightTypeTag

class WindowBuilder[A](
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
