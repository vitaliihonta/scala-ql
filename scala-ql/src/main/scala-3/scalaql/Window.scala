package scalaql

import scala.language.experimental.macros
import izumi.reflect.macrortti.LightTypeTag
import scala.util.hashing.MurmurHash3
import scalaql.internal.ChainedOrdering
import scala.quoted.*
import scalaql.utils.Scala3MacroUtils

class Window[A] @internalApi() (
  @internalApi() val __scalaql_window_partitions:    List[A => Any],
  @internalApi() val __scalaql_window_partitionTags: List[LightTypeTag],
  @internalApi() val __scalaql_window_orders:        List[(A => Any, Ordering[Any])],
  @internalApi() val __scalaql_window_orderTags:     List[LightTypeTag]) { self =>

  private[scalaql] def getPartitionKey(value: A): Int =
    MurmurHash3.orderedHash(
      __scalaql_window_partitions.map(_.apply(value)).reverse
    )

  private[scalaql] def ordering: Ordering[A] = new ChainedOrdering[A](__scalaql_window_orders.reverse)
}

object Window {
  import Scala3MacroUtils.*

  private val initial: WindowBuilder[Any] = new WindowBuilder[Any](Nil, Nil)

  def apply[A]: WindowBuilder[A] = initial.asInstanceOf[WindowBuilder[A]]

  extension [A](self: Window[A]) {

    inline def orderBy[B](f: A => B)(implicit tagB: Tag[B], orderingB: Ordering[B]): Window[A] =
      ${ Window.orderByImpl[A, B]('self, 'f, 'orderingB, 'tagB) }

    inline def orderBy[B, C](
      f1:            A => B,
      f2:            A => C
    )(implicit tagB: Tag[B],
      tagC:          Tag[C],
      orderingB:     Ordering[B],
      orderingC:     Ordering[C]
    ): Window[A] =
      ${ Window.orderByImpl2[A, B, C]('self, 'f1, 'f2, 'orderingB, 'orderingC, 'tagB, 'tagC) }

    inline def orderBy[B, C, D](
      f1:            A => B,
      f2:            A => C,
      f3:            A => D
    )(implicit tagB: Tag[B],
      tagC:          Tag[C],
      tagD:          Tag[D],
      orderingB:     Ordering[B],
      orderingC:     Ordering[C],
      orderingD:     Ordering[D]
    ): Window[A] =
      ${
        Window.orderByImpl3[A, B, C, D]('self, 'f1, 'f2, 'f3, 'orderingB, 'orderingC, 'orderingD, 'tagB, 'tagC, 'tagD)
      }

  }

  def orderByImpl[A: Type, B: Type](
    self:      Expr[Window[A]],
    f:         Expr[A => B],
    orderingB: Expr[Ordering[B]],
    TagB:      Expr[Tag[B]]
  )(using Quotes
  ): Expr[Window[A]] = {
    val resultOrderingB = getOrdering(accessorCallPath(f, ignoreUnmatched), orderingB)

    '{
      new Window[A](
        $self.__scalaql_window_partitions,
        $self.__scalaql_window_partitionTags,
        ${ makeOrder(f, resultOrderingB) } :: $self.__scalaql_window_orders,
        $TagB.tag :: $self.__scalaql_window_orderTags
      )
    }
  }

  def orderByImpl2[A: Type, B: Type, C: Type](
    self:      Expr[Window[A]],
    f1:        Expr[A => B],
    f2:        Expr[A => C],
    orderingB: Expr[Ordering[B]],
    orderingC: Expr[Ordering[C]],
    TagB:      Expr[Tag[B]],
    TagC:      Expr[Tag[C]]
  )(using Quotes
  ): Expr[Window[A]] = {
    val resultOrderingB = getOrdering(accessorCallPath(f1, ignoreUnmatched), orderingB)
    val resultOrderingC = getOrdering(accessorCallPath(f2, ignoreUnmatched), orderingC)

    '{
      new Window[A](
        $self.__scalaql_window_partitions,
        $self.__scalaql_window_partitionTags,
        ${ makeOrder(f2, resultOrderingC) } :: ${ makeOrder(f1, resultOrderingB) } :: $self.__scalaql_window_orders,
        $TagC.tag :: $TagB.tag :: $self.__scalaql_window_orderTags
      )
    }
  }

  def orderByImpl3[A: Type, B: Type, C: Type, D: Type](
    self:      Expr[Window[A]],
    f1:        Expr[A => B],
    f2:        Expr[A => C],
    f3:        Expr[A => D],
    orderingB: Expr[Ordering[B]],
    orderingC: Expr[Ordering[C]],
    orderingD: Expr[Ordering[D]],
    TagB:      Expr[Tag[B]],
    TagC:      Expr[Tag[C]],
    TagD:      Expr[Tag[D]]
  )(using Quotes
  ): Expr[Window[A]] = {
    val resultOrderingB = getOrdering(accessorCallPath(f1, ignoreUnmatched), orderingB)
    val resultOrderingC = getOrdering(accessorCallPath(f2, ignoreUnmatched), orderingC)
    val resultOrderingD = getOrdering(accessorCallPath(f3, ignoreUnmatched), orderingD)

    '{
      new Window[A](
        $self.__scalaql_window_partitions,
        $self.__scalaql_window_partitionTags,
        ${ makeOrder(f3, resultOrderingD) } :: ${ makeOrder(f2, resultOrderingC) }
          :: ${ makeOrder(f1, resultOrderingB) } :: $self.__scalaql_window_orders,
        $TagD.tag :: $TagC.tag :: $TagB.tag :: $self.__scalaql_window_orderTags
      )
    }
  }

  private def makeOrder[A: Type, B: Type](
    f:        Expr[A => B],
    ordering: Expr[Ordering[B]]
  )(using Quotes
  ): Expr[(A => Any, Ordering[Any])] =
    '{ ($f, $ordering.asInstanceOf[Ordering[Any]]) }
}
