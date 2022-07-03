package scalaql.internal

import scalaql.utils.{MathUtils, TupleFlatten}
import scalaql.{Aggregation, QueryExpressionBuilder, forbiddenInheritance}
import spire.algebra.{AdditiveMonoid, Field, MultiplicativeMonoid}
import spire.math.Fractional

@forbiddenInheritance
trait AggregationFunctions {

  final class Const[A](value: A) extends Aggregation[Any] {

    override type Out = A

    override def apply(xs: Iterable[Any]): A = value
  }

  final class Contramapped[A0, A, B](base: Aggregation.Of[A, B], f: A0 => A) extends Aggregation[A0] {

    override type Out = B

    override def apply(xs: Iterable[A0]): B = base.apply(xs.map(f))
  }

  final class Mapped[A, Out0, B](base: Aggregation.Of[A, Out0], f: Out0 => B) extends Aggregation[A] {
    override type Out = B

    override def apply(xs: Iterable[A]): B = f(base.apply(xs))
  }

  final class Chained[A, Out0, Out1, U](
    f:      Aggregation.Of[A, Out0],
    g:      Aggregation.Of[A, Out1]
  )(tupled: TupleFlatten.Of[(Out0, Out1), U])
      extends Aggregation[A] {

    override type Out = U

    override def apply(xs: Iterable[A]): U = tupled.apply((f(xs), g(xs)))
  }

  final class ToList[A] extends Aggregation[A] {
    override type Out = List[A]

    override def apply(xs: Iterable[A]): List[A] = xs.toList
  }

  final class ToListBy[A, B](f: A => B) extends Aggregation[A] {
    override type Out = List[B]

    override def apply(xs: Iterable[A]): List[B] = xs.map(f).toList
  }

  final class Distinct[A] extends Aggregation[A] {
    override type Out = Set[A]

    override def apply(xs: Iterable[A]): Set[A] = xs.toSet
  }

  final class DistinctBy[A, B](f: A => B) extends Aggregation[A] {
    override type Out = Set[B]

    override def apply(xs: Iterable[A]): Set[B] = xs.map(f).toSet
  }

  final class FlatDistinctBy[A, B](f: A => Iterable[B]) extends Aggregation[A] {
    override type Out = Set[B]

    override def apply(xs: Iterable[A]): Set[B] = xs.flatMap(f).toSet
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

  final class Std[A](ev: Fractional[A]) extends Aggregation[A] {

    override type Out = A

    override def apply(xs: Iterable[A]): A =
      MathUtils.std[A](xs)(ev).value
  }

  final class StdBy[A, B](f: A => B, ev: Fractional[B]) extends Aggregation[A] {

    override type Out = B

    override def apply(xs: Iterable[A]): B =
      MathUtils.std[B](xs.map(f))(ev).value
  }

  final class Min[A](ev: Ordering[A]) extends Aggregation[A] {

    override type Out = A

    override def apply(xs: Iterable[A]): A =
      xs.min(ev)
  }

  final class MinOf[A, B](f: A => B, ev: Ordering[B]) extends Aggregation[A] {

    override type Out = B

    override def apply(xs: Iterable[A]): B = {
      val iter = xs.iterator
      var min  = f(iter.next())
      while (iter.hasNext)
        min = ev.min(f(iter.next()), min)
      min
    }
  }

  final class Max[A](ev: Ordering[A]) extends Aggregation[A] {

    override type Out = A

    override def apply(xs: Iterable[A]): A =
      xs.max(ev)
  }

  final class MaxOf[A, B](f: A => B, ev: Ordering[B]) extends Aggregation[A] {

    override type Out = B

    override def apply(xs: Iterable[A]): B = {
      val iter = xs.iterator
      var max  = f(iter.next())
      while (iter.hasNext)
        max = ev.max(f(iter.next()), max)
      max
    }
  }

  final class Reduce[A](f: (A, A) => A) extends Aggregation[A] {
    override type Out = A

    override def apply(xs: Iterable[A]): A =
      xs.reduce(f)
  }

  final class ReduceBy[A, B](by: A => B)(f: (B, B) => B) extends Aggregation[A] {
    override type Out = B

    override def apply(xs: Iterable[A]): B = {
      val iter = xs.iterator
      var res  = by(iter.next())
      while (iter.hasNext)
        res = f(res, by(iter.next()))
      res
    }
  }

  final class FoldLeft[A, B](initial: B, f: (B, A) => B) extends Aggregation[A] {
    override type Out = B

    override def apply(xs: Iterable[A]): B =
      xs.foldLeft(initial)(f)
  }

  final class FoldLeftBy[A, B, R](by: A => B)(initial: R, f: (R, B) => R) extends Aggregation[A] {
    override type Out = R

    override def apply(xs: Iterable[A]): R = {
      val iter = xs.iterator
      var res  = initial
      while (iter.hasNext)
        res = f(res, by(iter.next()))
      res
    }
  }

  final class Report1[A, B, U1](
    view1:  QueryExpressionBuilder[A]
  )(group1: A => B,
    agg1:   (B, QueryExpressionBuilder[A]) => Aggregation.Of[A, U1])
      extends Aggregation[A] {

    override type Out = List[U1]

    override def apply(xs: Iterable[A]): List[U1] =
      xs.groupBy(group1)
        .view
        .map { case (sliceB, bs) =>
          agg1(sliceB, view1).apply(bs)
        }
        .toList
  }

  final class Report2[A, B, C, U1, U2](
    view1:  QueryExpressionBuilder[A],
    view2:  QueryExpressionBuilder[U1]
  )(group1: A => B,
    group2: A => C,
    agg1:   (B, C, QueryExpressionBuilder[A]) => Aggregation.Of[A, U1],
    agg2:   (B, QueryExpressionBuilder[U1]) => Aggregation.Of[U1, U2])
      extends Aggregation[A] {

    override type Out = List[U2]

    override def apply(xs: Iterable[A]): List[U2] =
      xs.groupBy(group1)
        .view
        .map { case (sliceB, bs) =>
          val merged1 = bs.groupBy(group2).view.map { case (sliceC, cs) =>
            agg1(sliceB, sliceC, view1).apply(cs)
          }
          agg2(sliceB, view2).apply(merged1.toList)
        }
        .toList
  }
}
