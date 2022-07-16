package scalaql.syntax

import scalaql.*
import scala.quoted.*
import scalaql.utils.Scala3MacroUtils

trait OrderingSyntax {
  extension [A](self: A) {

    /** Use ascending order */
    def asc: A = self

    /** Use descending order */
    def desc: A = self
  }

  extension [In, Out](self: Query[In, Out]) {

    /**
     * Orders `this` query output values by their natural order.
     *
     * @param order the natural order of output values
     * @return query emitting values in the specified order
     * */
    def ordered(implicit order: Ordering[Out], In: Tag[In], Out: Tag[Out]): Query[In, Out] =
      new Query.OrderByQuery[In, Out, Out](self, identity, None)

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
    inline def orderBy[B](
      f:                  Out => B
    )(implicit orderingB: Ordering[B],
      In:                 Tag[In],
      Out:                Tag[Out]
    ): Query[In, Out] =
      ${ OrderingSyntax.orderByImpl[In, Out, B]('self, 'f, 'orderingB, 'In, 'Out) }

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
    inline def orderBy[B, C](
      f1:                 Out => B,
      f2:                 Out => C
    )(implicit orderingB: Ordering[B],
      orderingC:          Ordering[C],
      In:                 Tag[In],
      Out:                Tag[Out]
    ): Query[In, Out] =
      ${ OrderingSyntax.orderByImpl2[In, Out, B, C]('self, 'f1, 'f2, 'orderingB, 'orderingC, 'In, 'Out) }

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
    inline def orderBy[B, C, D](
      f1:                 Out => B,
      f2:                 Out => C,
      f3:                 Out => D
    )(implicit orderingB: Ordering[B],
      orderingC:          Ordering[C],
      orderingD:          Ordering[D],
      In:                 Tag[In],
      Out:                Tag[Out]
    ): Query[In, Out] =
      ${
        OrderingSyntax.orderByImpl3[In, Out, B, C, D](
          'self,
          'f1,
          'f2,
          'f3,
          'orderingB,
          'orderingC,
          'orderingD,
          'In,
          'Out
        )
      }
  }
}

object OrderingSyntax {
  import Scala3MacroUtils.*

  def orderByImpl[In: Type, Out: Type, B: Type](
    self:     Expr[Query[In, Out]],
    f:        Expr[Out => B],
    ordering: Expr[Ordering[B]],
    In:       Expr[Tag[In]],
    Out:      Expr[Tag[Out]]
  )(using q:  Quotes
  ): Expr[Query[In, Out]] = {
    val resultOrdering = getOrdering(accessorCallPath(f, ignoreUnmatched), ordering)

    val By = '{ Tag[B] }

    val makeOrderBy  = f
    val makeOrdering = resultOrdering

    '{
      new Query.OrderByQuery[In, Out, B]($self, $makeOrderBy, Some($By.tag))($In, $Out, $makeOrdering)
    }
  }

  def orderByImpl2[In: Type, Out: Type, B: Type, C: Type](
    self:      Expr[Query[In, Out]],
    f1:        Expr[Out => B],
    f2:        Expr[Out => C],
    orderingB: Expr[Ordering[B]],
    orderingC: Expr[Ordering[C]],
    In:        Expr[Tag[In]],
    Out:       Expr[Tag[Out]]
  )(using q:   Quotes
  ): Expr[Query[In, Out]] = {
    val resultOrderingB = getOrdering(accessorCallPath(f1, ignoreUnmatched), orderingB)
    val resultOrderingC = getOrdering(accessorCallPath(f2, ignoreUnmatched), orderingC)

    val By           = '{ Tag[(B, C)] }
    val makeOrderBy  = '{ (out: Out) => ($f1(out), $f2(out)) }
    val makeOrdering = '{ Ordering.Tuple2($resultOrderingB, $resultOrderingC) }

    '{
      new Query.OrderByQuery[In, Out, (B, C)]($self, $makeOrderBy, Some($By.tag))($In, $Out, $makeOrdering)
    }
  }

  def orderByImpl3[In: Type, Out: Type, B: Type, C: Type, D: Type](
    self:      Expr[Query[In, Out]],
    f1:        Expr[Out => B],
    f2:        Expr[Out => C],
    f3:        Expr[Out => D],
    orderingB: Expr[Ordering[B]],
    orderingC: Expr[Ordering[C]],
    orderingD: Expr[Ordering[D]],
    In:        Expr[Tag[In]],
    Out:       Expr[Tag[Out]]
  )(using q:   Quotes
  ): Expr[Query[In, Out]] = {
    val resultOrderingB = getOrdering(accessorCallPath(f1, ignoreUnmatched), orderingB)
    val resultOrderingC = getOrdering(accessorCallPath(f2, ignoreUnmatched), orderingC)
    val resultOrderingD = getOrdering(accessorCallPath(f3, ignoreUnmatched), orderingD)

    val By           = '{ Tag[(B, C, D)] }
    val makeOrderBy  = '{ (out: Out) => ($f1(out), $f2(out), $f3(out)) }
    val makeOrdering = '{ Ordering.Tuple3($resultOrderingB, $resultOrderingC, $resultOrderingD) }

    '{
      new Query.OrderByQuery[In, Out, (B, C, D)]($self, $makeOrderBy, Some($By.tag))($In, $Out, $makeOrdering)
    }
  }
}
