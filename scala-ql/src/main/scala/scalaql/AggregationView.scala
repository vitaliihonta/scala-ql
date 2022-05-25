package scalaql

import scalaql.Aggregation.Aux
import spire.algebra.AdditiveMonoid
import spire.algebra.Field
import spire.algebra.MultiplicativeMonoid

sealed trait AggregationDsl[In, Out] {
  def toList: Aggregation.Aux[In, List[Out]]

  def distinct: Aggregation.Aux[In, Set[Out]]

  def const[B](value: B): Aggregation.Aux[In, B]

  def sum(implicit ev: AdditiveMonoid[Out]): Aggregation.Aux[In, Out]
  def sumBy[B](f:      Out => B)(implicit ev: AdditiveMonoid[B]): Aggregation.Aux[In, B]

  def product(implicit ev: MultiplicativeMonoid[Out]): Aggregation.Aux[In, Out]
  def productBy[B](f:      Out => B)(implicit ev: MultiplicativeMonoid[B]): Aggregation.Aux[In, B]

  def avg(implicit ev: Field[Out]): Aggregation.Aux[In, Out]
  def avgBy[B](f:      Out => B)(implicit ev: Field[B]): Aggregation.Aux[In, B]

  def count(p: Out => Boolean): Aggregation.Aux[In, Int]

  def size: Aggregation.Aux[In, Int] = count(_ => true)

  def custom[B](f: Iterable[Out] => B): Aggregation.Aux[In, B]

  def report[B, C, U1, U2](
    split1:        Out => B,
    split2:        Out => C
  )(mergeCascade1: (B, C, AggregationView[Out]) => Aggregation.Aux[Out, U1]
  )(mergeCascade2: (B, AggregationView[U1]) => Aggregation.Aux[U1, U2]
  ): Aggregation.Aux[In, List[U2]]
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

  private[scalaql] abstract class Impl[A] extends AggregationView[A] {
    protected def mkChild[U]: AggregationView[U]

    override def map[B](f: A => B): AggregationViewMapped[A, B] =
      new MappedImpl[A, B](mkChild[B])(f)

    override def const[B](value: B): Aggregation.Aux[A, B] =
      new Aggregation.Const[B](value)

    override def toList: Aggregation.Aux[A, List[A]] =
      new Aggregation.ToList[A]

    override def distinct: Aux[A, Set[A]] =
      new Aggregation.Distinct[A]

    override def count(p: A => Boolean): Aggregation.Aux[A, Int] =
      new Aggregation.Count[A](p)

    override def sum(implicit ev: AdditiveMonoid[A]): Aggregation.Aux[A, A] =
      new Aggregation.Sum[A](ev)

    override def sumBy[B](f: A => B)(implicit ev: AdditiveMonoid[B]): Aggregation.Aux[A, B] =
      new Aggregation.SumBy[A, B](f, ev)

    override def product(implicit ev: MultiplicativeMonoid[A]): Aggregation.Aux[A, A] =
      new Aggregation.Product[A](ev)

    override def productBy[B](f: A => B)(implicit ev: MultiplicativeMonoid[B]): Aggregation.Aux[A, B] =
      new Aggregation.ProductBy[A, B](f, ev)

    override def avg(implicit ev: Field[A]): Aggregation.Aux[A, A] =
      new Aggregation.Avg[A](ev)

    override def avgBy[B](f: A => B)(implicit ev: Field[B]): Aggregation.Aux[A, B] =
      new Aggregation.AvgBy[A, B](f, ev)

    def report[B, C, U1, U2](
      group1: A => B,
      group2: A => C
    )(agg1:   (B, C, AggregationView[A]) => Aggregation.Aux[A, U1]
    )(agg2:   (B, AggregationView[U1]) => Aggregation.Aux[U1, U2]
    ): Aggregation.Aux[A, List[U2]] =
      new Aggregation.Report2[A, B, C, U1, U2](mkChild[A], mkChild[U1])(
        group1,
        group2,
        agg1,
        agg2
      )

    override def custom[B](f: Iterable[A] => B): Aggregation.Aux[A, B] =
      new Aggregation.Custom[A, B](f)
  }

  private[scalaql] class MappedImpl[A, Out](delegate: AggregationView[Out])(project: A => Out)
      extends AggregationViewMapped[A, Out] {

    override def toList: Aggregation.Aux[A, List[Out]] =
      delegate.toList.contramap(project)

    override def distinct: Aux[A, Set[Out]] =
      delegate.distinct.contramap(project)

    override def const[B](value: B): Aggregation.Aux[A, B] =
      new Aggregation.Const[B](value)

    override def count(p: Out => Boolean): Aggregation.Aux[A, Int] =
      delegate.count(p).contramap(project)

    override def sum(implicit ev: AdditiveMonoid[Out]): Aggregation.Aux[A, Out] =
      delegate.sum.contramap(project)

    override def sumBy[B](f: Out => B)(implicit ev: AdditiveMonoid[B]): Aggregation.Aux[A, B] =
      delegate.sumBy(f).contramap(project)

    override def product(implicit ev: MultiplicativeMonoid[Out]): Aggregation.Aux[A, Out] =
      delegate.product.contramap(project)

    override def productBy[B](f: Out => B)(implicit ev: MultiplicativeMonoid[B]): Aggregation.Aux[A, B] =
      delegate.productBy(f).contramap(project)

    override def avg(implicit ev: Field[Out]): Aggregation.Aux[A, Out] =
      delegate.avg.contramap(project)

    override def avgBy[B](f: Out => B)(implicit ev: Field[B]): Aggregation.Aux[A, B] =
      delegate.avgBy(f).contramap(project)

    override def custom[B](f: Iterable[Out] => B): Aggregation.Aux[A, B] =
      delegate.custom(f).contramap(project)

    override def report[B, C, U1, U2](
      group1: Out => B,
      group2: Out => C
    )(agg1:   (B, C, AggregationView[Out]) => Aux[Out, U1]
    )(agg2:   (B, AggregationView[U1]) => Aux[U1, U2]
    ): Aggregation.Aux[A, List[U2]] =
      delegate
        .report(group1, group2)(agg1)(agg2)
        .contramap(project)
  }
}
