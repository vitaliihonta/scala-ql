package scalaql.syntax

import scalaql.*
import scalaql.internal.OrderBySyntaxMacro

import scala.language.experimental.macros

final class OrderBySyntax[In, Out](@internalApi val __scalaql_self: Query[In, Out]) extends AnyVal {

  /**
   * Orders `this` query output values by their natural order.
   *
   * @param order the natural order of output values
   * @return query emitting values in the specified order
   * */
  def ordered(implicit order: Ordering[Out], In: Tag[In], Out: Tag[Out]): Query[In, Out] =
    new Query.OrderByQuery[In, Out, Out](__scalaql_self, identity, None)

  /**
   * Orders `this` query output values by the specified ordering key.
   * Could be ordered either `ascending` or `descending`.
   * @note for `descending` order, it's required to use `.desc` method on the ordering key.
   *       `scalaql` will automatically reverse the implicit ordering.
   *
   * Example:
   * {{{
   *   select[Person].orderBy(_.age)
   *   // or
   *   select[Person].orderBy(_.age.desc)
   * }}}
   *
   * @tparam B ordering key type
   * @param f get the ordering key
   * @param orderingB ordering of the key
   * @return query emitting values in the specified order
   * */
  def orderBy[B](f: Out => B)(implicit orderingB: Ordering[B]): Query[In, Out] =
    macro OrderBySyntaxMacro.orderBy1[In, Out, B]

  /**
   * Orders `this` query output values by the specified ordering keys.
   * Could be ordered either `ascending` or `descending`.
   * @note for `descending` order, it's required to use `.desc` method on the ordering key.
   *       `scalaql` will automatically reverse the implicit ordering.
   *
   * Example:
   * {{{
   *   select[Person].orderBy(_.name, _.age)
   *   // or
   *   select[Person].orderBy(_.name, _.age.desc)
   * }}}
   *
   * @tparam B first ordering key type
   * @tparam C second ordering key type
   * @param f1 get the first ordering key
   * @param f2 get the second ordering key
   * @param orderingB ordering of the first key
   * @param orderingC ordering of the second key
   * @return query emitting values in the specified order
   * */
  def orderBy[B, C](
    f1:                 Out => B,
    f2:                 Out => C
  )(implicit orderingB: Ordering[B],
    orderingC:          Ordering[C]
  ): Query[In, Out] =
    macro OrderBySyntaxMacro.orderBy2[In, Out, B, C]

  /**
   * Orders `this` query output values by the specified ordering keys.
   * Could be ordered either `ascending` or `descending`.
   * @note for `descending` order, it's required to use `.desc` method on the ordering key.
   *       `scalaql` will automatically reverse the implicit ordering.
   *
   * Example:
   * {{{
   *   select[Person].orderBy(_.name, _.age, _.salary)
   *   // or
   *   select[Person].orderBy(_.name, _.age.desc, _.salary.desc)
   * }}}
   *
   * @tparam B first ordering key type
   * @tparam C second ordering key type
   * @tparam D third ordering key type
   * @param f1 get the first ordering key
   * @param f2 get the second ordering key
   * @param f3 get the third ordering key
   * @param orderingB ordering of the first key
   * @param orderingC ordering of the second key
   * @param orderingD ordering of the third key
   * @return query emitting values in the specified order
   * */
  def orderBy[B, C, D](
    f1:                 Out => B,
    f2:                 Out => C,
    f3:                 Out => D
  )(implicit orderingB: Ordering[B],
    orderingC:          Ordering[C],
    orderingD:          Ordering[D]
  ): Query[In, Out] =
    macro OrderBySyntaxMacro.orderBy3[In, Out, B, C, D]
}
