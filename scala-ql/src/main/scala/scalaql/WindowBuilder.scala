package scalaql

import izumi.reflect.macrortti.LightTypeTag

/**
 * Entrypoint for defining a window function.
 * Used inside an `over` clause when defining a windowed query:
 * `.over(_.partitionBy(_.country))`
 *
 * Example:
 * {{{
 *   select[Person]
 *     .window(_.rowNumber)
 *     .over(_.partitionBy(_.country))
 * }}}
 * */
class WindowBuilder[A](
  partitions:                         List[A => Any],
  private[scalaql] val partitionTags: List[LightTypeTag]) {

  /**
   * Partitions the query output over a single value.
   *
   * @tparam B partitioned value type
   * @param f get value for partitioning
   * @return window description
   * */
  def partitionBy[B: Tag](f: A => B): Window[A] =
    new Window[A](
      f :: partitions,
      Tag[B].tag :: partitionTags,
      __scalaql_window_orders = Nil,
      __scalaql_window_orderTags = Nil
    )

  /**
   * Partitions the query output over two values.
   *
   * @tparam B first partitioned value type
   * @tparam C second partitioned value type
   * @param f1 get the first value for partitioning
   * @param f2 get the second value for partitioning
   * @return window description
   * */
  def partitionBy[B: Tag, C: Tag](f1: A => B, f2: A => C): Window[A] =
    new Window[A](
      f2 :: f1 :: partitions,
      Tag[C].tag :: Tag[B].tag :: partitionTags,
      __scalaql_window_orders = Nil,
      __scalaql_window_orderTags = Nil
    )

  /**
   * Partitions the query output over three values.
   *
   * @tparam B first partitioned value type
   * @tparam C second partitioned value type
   * @tparam D third partitioned value type
   * @param f1 get the first value for partitioning
   * @param f2 get the second value for partitioning
   * @param f3 get the third value for partitioning
   * @return window description
   * */
  def partitionBy[B: Tag, C: Tag, D: Tag](f1: A => B, f2: A => C, f3: A => D): Window[A] =
    new Window[A](
      f3 :: f2 :: f1 :: partitions,
      Tag[D].tag :: Tag[C].tag :: Tag[B].tag :: partitionTags,
      __scalaql_window_orders = Nil,
      __scalaql_window_orderTags = Nil
    )

  /**
   * Partitions the query output over four values.
   *
   * @tparam B first partitioned value type
   * @tparam C second partitioned value type
   * @tparam D third partitioned value type
   * @tparam E fourth partitioned value type
   * @param f1 get the first value for partitioning
   * @param f2 get the second value for partitioning
   * @param f3 get the third value for partitioning
   * @param f4 get the fourth value for partitioning
   * @return window description
   * */
  def partitionBy[B: Tag, C: Tag, D: Tag, E: Tag](f1: A => B, f2: A => C, f3: A => D, f4: A => E): Window[A] =
    new Window[A](
      f4 :: f3 :: f2 :: f1 :: partitions,
      Tag[E].tag :: Tag[D].tag :: Tag[C].tag :: Tag[B].tag :: partitionTags,
      __scalaql_window_orders = Nil,
      __scalaql_window_orderTags = Nil
    )
}
