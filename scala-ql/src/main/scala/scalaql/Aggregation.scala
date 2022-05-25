package scalaql

import scalaql.utils.TupleFlatten
import spire.algebra.AdditiveMonoid
import spire.algebra.Field
import spire.algebra.MultiplicativeMonoid
import scala.annotation.unchecked.uncheckedVariance

sealed trait Aggregation[-A] { self =>
  type Out

  def apply(xs: Iterable[A]): Out

  def contramap[A0](f: A0 => A): Aggregation.Aux[A0, Out] =
    new Aggregation.Contramapped[A0, A, Out](self, f)

  def map[B](f: Out => B): Aggregation.Aux[A, B] =
    new Aggregation.Mapped[A, Out, B](self, f)

  def &&[A0 <: A](
    that:            Aggregation[A0]
  )(implicit tupled: TupleFlatten[(Out, that.Out)]
  ): Aggregation.Aux[A0, tupled.Out] =
    new Aggregation.Chained[A0, Out, that.Out, tupled.Out](self, that)(tupled)
}

object Aggregation {
  type Aux[-A, +Out0] = Aggregation[A] { type Out = Out0 @uncheckedVariance }

  final class Const[A](value: A) extends Aggregation[Any] {

    override type Out = A

    override def apply(xs: Iterable[Any]): A = value
  }

  final class Contramapped[A0, A, B](base: Aggregation.Aux[A, B], f: A0 => A) extends Aggregation[A0] {

    override type Out = B

    override def apply(xs: Iterable[A0]): B = base.apply(xs.map(f))
  }

  final class Mapped[A, Out0, B](base: Aggregation.Aux[A, Out0], f: Out0 => B) extends Aggregation[A] {
    override type Out = B

    override def apply(xs: Iterable[A]): B = f(base.apply(xs))
  }

  final class Chained[A, Out0, Out1, U](
    f:      Aggregation.Aux[A, Out0],
    g:      Aggregation.Aux[A, Out1]
  )(tupled: TupleFlatten.Aux[(Out0, Out1), U])
      extends Aggregation[A] {

    override type Out = U

    override def apply(xs: Iterable[A]): U = tupled.apply((f(xs), g(xs)))
  }

  final class ToList[A] extends Aggregation[A] {
    override type Out = List[A]

    override def apply(xs: Iterable[A]): List[A] = xs.toList
  }

  final class Distinct[A] extends Aggregation[A] {
    override type Out = Set[A]

    override def apply(xs: Iterable[A]): Set[A] = xs.toSet
  }

  final class Sum[A](ev: AdditiveMonoid[A]) extends Aggregation[A] {
    override type Out = A

    override def apply(xs: Iterable[A]): A = ev.additive.combineAll(xs)
  }

  final class Count[A](p: A => Boolean) extends Aggregation[A] {
    override type Out = Int

    override def apply(xs: Iterable[A]): Int = xs.count(p)
  }

  final class Product[A](ev: MultiplicativeMonoid[A]) extends Aggregation[A] {
    override type Out = A

    override def apply(xs: Iterable[A]): A = ev.multiplicative.combineAll(xs)
  }

  final class SumBy[A, B](f: A => B, ev: AdditiveMonoid[B]) extends Aggregation[A] {
    override type Out = B

    override def apply(xs: Iterable[A]): B = {
      var res = ev.zero
      for (x <- xs) res = ev.additive.combine(res, f(x))
      res
    }
  }

  final class ProductBy[A, B](f: A => B, ev: MultiplicativeMonoid[B]) extends Aggregation[A] {

    override type Out = B

    override def apply(xs: Iterable[A]): B = {
      var res = ev.one
      for (x <- xs) res = ev.multiplicative.combine(res, f(x))
      res
    }
  }

  final class Avg[A](ev: Field[A]) extends Aggregation[A] {

    override type Out = A

    override def apply(xs: Iterable[A]): A =
      ev.div(ev.sum(xs), ev.fromInt(xs.size))
  }

  final class AvgBy[A, B](f: A => B, ev: Field[B]) extends Aggregation[A] {

    override type Out = B

    override def apply(xs: Iterable[A]): B = {
      var sum = ev.zero
      for (x <- xs) sum = ev.additive.combine(sum, f(x))
      ev.div(sum, ev.fromInt(xs.size))
    }
  }

  final class Report2[A, B, C, U1, U2](
    view1:  AggregationView[A],
    view2:  AggregationView[U1]
  )(group1: A => B,
    group2: A => C,
    agg1:   (B, C, AggregationView[A]) => Aggregation.Aux[A, U1],
    agg2:   (B, AggregationView[U1]) => Aggregation.Aux[U1, U2])
      extends Aggregation[A] {

    override type Out = List[U2]

    override def apply(xs: Iterable[A]): List[U2] =
      xs.groupBy(group1)
        .view
        .map {
          case (sliceB, bs) =>
            val merged1 = bs.groupBy(group2).view.map {
              case (sliceC, cs) =>
                agg1(sliceB, sliceC, view1).apply(cs)
            }
            agg2(sliceB, view2).apply(merged1.toList)
        }
        .toList
  }

  final class Custom[A, B](f: Iterable[A] => B) extends Aggregation[A] {
    override type Out = B

    override def apply(xs: Iterable[A]): B = f(xs)
  }
}
