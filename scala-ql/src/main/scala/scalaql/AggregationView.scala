package scalaql

import scalaql.syntax.{ReportPartiallyApplied2, ReportPartiallyApplied2Syntax}
import spire.algebra.AdditiveMonoid
import spire.algebra.Field
import spire.algebra.MultiplicativeMonoid
import spire.math.Fractional

sealed trait AggregationDsl[In, Out] extends Serializable {
  def toList: Aggregation.Of[In, List[Out]]

  def distinct: Aggregation.Of[In, Set[Out]]

  def distinctBy[B](f:     Out => B): Aggregation.Of[In, Set[B]]
  def flatDistinctBy[B](f: Out => Iterable[B]): Aggregation.Of[In, Set[B]]

  def const[B](value: B): Aggregation.Of[In, B]

  def sum(implicit ev: AdditiveMonoid[Out]): Aggregation.Of[In, Out]
  def sumBy[B](f:      Out => B)(implicit ev: AdditiveMonoid[B]): Aggregation.Of[In, B]

  def product(implicit ev: MultiplicativeMonoid[Out]): Aggregation.Of[In, Out]
  def productBy[B](f:      Out => B)(implicit ev: MultiplicativeMonoid[B]): Aggregation.Of[In, B]

  def avg(implicit ev: Field[Out]): Aggregation.Of[In, Out]
  def avgBy[B](f:      Out => B)(implicit ev: Field[B]): Aggregation.Of[In, B]

  def std(implicit ev: Fractional[Out]): Aggregation.Of[In, Out]
  def stdBy[B](f:      Out => B)(implicit ev: Fractional[B]): Aggregation.Of[In, B]

  def count(p: Out => Boolean): Aggregation.Of[In, Int]

  def size: Aggregation.Of[In, Int] = count(_ => true)

  def reduce(f: (Out, Out) => Out): Aggregation.Of[In, Out]

  def foldLeft[B](initial: B)(f: (B, Out) => B): Aggregation.Of[In, B]

  def custom[B](f: Iterable[Out] => B): Aggregation.Of[In, B]

  def report[B, U1](
    group1: Out => B
  )(agg1:   (B, AggregationView[Out]) => Aggregation.Of[Out, U1]
  ): Aggregation.Of[In, List[U1]]

  def report[B, C, U1](
    group1: Out => B,
    group2: Out => C
  )(merge:  (B, C, AggregationView[Out]) => Aggregation.Of[Out, U1]
  ): ReportPartiallyApplied2[Out, B, C, U1]
}

sealed trait AggregationView[A] extends AggregationDsl[A, A] {
  def map[B](f: A => B): AggregationViewMapped[A, B]

  def as[B](value: B): AggregationViewMapped[A, B] = map(_ => value)
}

sealed trait AggregationViewMapped[A, B] extends AggregationDsl[A, B]

object AggregationView {
  private[scalaql] def create[A]: AggregationView[A] = singletonImpl.mkChild[A]

  private object singletonImpl extends Impl[Nothing] { self =>

    override final def mkChild[U]: AggregationView[U] =
      self.asInstanceOf[AggregationView[U]]
  }

  private[scalaql] abstract class Impl[A] extends AggregationView[A] with ReportPartiallyApplied2Syntax[A] {
    protected def mkChild[U]: AggregationView[U]

    override def map[B](f: A => B): AggregationViewMapped[A, B] =
      new MappedImpl[A, B](mkChild[B])(f)

    override def const[B](value: B): Aggregation.Of[A, B] =
      new Aggregation.Const[B](value)

    override def toList: Aggregation.Of[A, List[A]] =
      new Aggregation.ToList[A]

    override def distinct: Aggregation.Of[A, Set[A]] =
      new Aggregation.Distinct[A]

    override def distinctBy[B](f: A => B): Aggregation.Of[A, Set[B]] =
      new Aggregation.DistinctBy[A, B](f)

    override def flatDistinctBy[B](f: A => Iterable[B]): Aggregation.Of[A, Set[B]] =
      new Aggregation.FlatDistinctBy[A, B](f)

    override def count(p: A => Boolean): Aggregation.Of[A, Int] =
      new Aggregation.Count[A](p)

    override def sum(implicit ev: AdditiveMonoid[A]): Aggregation.Of[A, A] =
      new Aggregation.Sum[A](ev)

    override def sumBy[B](f: A => B)(implicit ev: AdditiveMonoid[B]): Aggregation.Of[A, B] =
      new Aggregation.SumBy[A, B](f, ev)

    override def product(implicit ev: MultiplicativeMonoid[A]): Aggregation.Of[A, A] =
      new Aggregation.Product[A](ev)

    override def productBy[B](f: A => B)(implicit ev: MultiplicativeMonoid[B]): Aggregation.Of[A, B] =
      new Aggregation.ProductBy[A, B](f, ev)

    override def avg(implicit ev: Field[A]): Aggregation.Of[A, A] =
      new Aggregation.Avg[A](ev)

    override def avgBy[B](f: A => B)(implicit ev: Field[B]): Aggregation.Of[A, B] =
      new Aggregation.AvgBy[A, B](f, ev)

    override def std(implicit ev: Fractional[A]): Aggregation.Of[A, A] =
      new Aggregation.Std[A](ev)

    override def stdBy[B](f: A => B)(implicit ev: Fractional[B]): Aggregation.Of[A, B] =
      new Aggregation.StdBy[A, B](f, ev)

    override def reduce(f: (A, A) => A): Aggregation.Of[A, A] =
      new Aggregation.Reduce[A](f)

    override def foldLeft[B](initial: B)(f: (B, A) => B): Aggregation.Of[A, B] =
      new Aggregation.FoldLeft[A, B](initial, f)

    override def report[B, U1](
      group1: A => B
    )(agg1:   (B, AggregationView[A]) => Aggregation.Of[A, U1]
    ): Aggregation.Of[A, List[U1]] =
      new Aggregation.Report1[A, B, U1](mkChild[A])(group1, agg1)

    override def custom[B](f: Iterable[A] => B): Aggregation.Of[A, B] =
      new Aggregation.Custom[A, B](f)
  }

  private[scalaql] class MappedImpl[A, Out](delegate: AggregationView[Out])(project: A => Out)
      extends AggregationViewMapped[A, Out] {

    override def toList: Aggregation.Of[A, List[Out]] =
      delegate.toList.contramap(project)

    override def distinct: Aggregation.Of[A, Set[Out]] =
      delegate.distinct.contramap(project)

    override def distinctBy[B](f: Out => B): Aggregation.Of[A, Set[B]] =
      delegate.distinctBy(f).contramap(project)

    override def flatDistinctBy[B](f: Out => Iterable[B]): Aggregation.Of[A, Set[B]] =
      delegate.flatDistinctBy(f).contramap(project)

    override def const[B](value: B): Aggregation.Of[A, B] =
      new Aggregation.Const[B](value)

    override def count(p: Out => Boolean): Aggregation.Of[A, Int] =
      delegate.count(p).contramap(project)

    override def sum(implicit ev: AdditiveMonoid[Out]): Aggregation.Of[A, Out] =
      delegate.sum.contramap(project)

    override def sumBy[B](f: Out => B)(implicit ev: AdditiveMonoid[B]): Aggregation.Of[A, B] =
      delegate.sumBy(f).contramap(project)

    override def product(implicit ev: MultiplicativeMonoid[Out]): Aggregation.Of[A, Out] =
      delegate.product.contramap(project)

    override def productBy[B](f: Out => B)(implicit ev: MultiplicativeMonoid[B]): Aggregation.Of[A, B] =
      delegate.productBy(f).contramap(project)

    override def avg(implicit ev: Field[Out]): Aggregation.Of[A, Out] =
      delegate.avg.contramap(project)

    override def avgBy[B](f: Out => B)(implicit ev: Field[B]): Aggregation.Of[A, B] =
      delegate.avgBy(f).contramap(project)

    override def std(implicit ev: Fractional[Out]): Aggregation.Of[A, Out] =
      delegate.std.contramap(project)

    override def stdBy[B](f: Out => B)(implicit ev: Fractional[B]): Aggregation.Of[A, B] =
      delegate.stdBy(f).contramap(project)

    override def reduce(f: (Out, Out) => Out): Aggregation.Of[A, Out] =
      delegate.reduce(f).contramap(project)

    override def foldLeft[B](initial: B)(f: (B, Out) => B): Aggregation.Of[A, B] =
      delegate.foldLeft(initial)(f).contramap(project)

    override def custom[B](f: Iterable[Out] => B): Aggregation.Of[A, B] =
      delegate.custom(f).contramap(project)

    override def report[B, U1](
      group1: Out => B
    )(agg1:   (B, AggregationView[Out]) => Aggregation.Of[Out, U1]
    ): Aggregation.Of[A, List[U1]] =
      delegate
        .report[B, U1](group1)(agg1)
        .contramap(project)

    override def report[B, C, U1](
      group1: Out => B,
      group2: Out => C
    )(merge:  (B, C, AggregationView[Out]) => Aggregation.Of[Out, U1]
    ): ReportPartiallyApplied2[Out, B, C, U1] =
      delegate.report[B, C, U1](group1, group2)(merge)
  }
}
