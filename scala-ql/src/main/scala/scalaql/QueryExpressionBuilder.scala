package scalaql

import spire.algebra.{AdditiveMonoid, Field, MultiplicativeMonoid}
import spire.math.Fractional

sealed trait QueryExpressionBuilder[A] extends Serializable {
  def toList: Aggregation.Of[A, List[A]]
  def toListOf[B](f: A => B): Aggregation.Of[A, List[B]]

  def distinct: Aggregation.Of[A, Set[A]]

  def distinctBy[B](f:     A => B): Aggregation.Of[A, Set[B]]
  def flatDistinctBy[B](f: A => Iterable[B]): Aggregation.Of[A, Set[B]]

  def const[B](value: B): Aggregation.Of[A, B]

  def min(implicit ev: Ordering[A]): Aggregation.Of[A, A]
  def minOf[B](f:      A => B)(implicit ev: Ordering[B]): Aggregation.Of[A, B]

  def max(implicit ev: Ordering[A]): Aggregation.Of[A, A]
  def maxOf[B](f:      A => B)(implicit ev: Ordering[B]): Aggregation.Of[A, B]

  def sum(implicit ev: AdditiveMonoid[A]): Aggregation.Of[A, A]
  def sumBy[B](f:      A => B)(implicit ev: AdditiveMonoid[B]): Aggregation.Of[A, B]

  def product(implicit ev: MultiplicativeMonoid[A]): Aggregation.Of[A, A]
  def productBy[B](f:      A => B)(implicit ev: MultiplicativeMonoid[B]): Aggregation.Of[A, B]

  def avg(implicit ev: Field[A]): Aggregation.Of[A, A]
  def avgBy[B](f:      A => B)(implicit ev: Field[B]): Aggregation.Of[A, B]

  def std(implicit ev: Fractional[A]): Aggregation.Of[A, A]
  def stdBy[B](f:      A => B)(implicit ev: Fractional[B]): Aggregation.Of[A, B]

  def count(p: A => Boolean): Aggregation.Of[A, Int]

  def size: Aggregation.Of[A, Int] = count(_ => true)

  def rowNumber: Ranking.Of[A, Int]

  def rank: Ranking.Of[A, Int]

  def lag[B](f: A => B): Ranking.Of[A, Option[B]]

  def reduce(f: (A, A) => A): Aggregation.Of[A, A]

  def reduceBy[B](by: A => B)(f: (B, B) => B): Aggregation.Of[A, B]

  def foldLeft[B](initial: B)(f: (B, A) => B): Aggregation.Of[A, B]

  def foldLeftBy[B, R](by: A => B)(initial: R)(f: (R, B) => R): Aggregation.Of[A, R]
}

object QueryExpressionBuilder {

  private[scalaql] def create[A]: QueryExpressionBuilder[A] = singletonImpl.asInstanceOf[QueryExpressionBuilder[A]]

  private object singletonImpl extends Impl[Nothing]

  private[scalaql] abstract class Impl[A] extends QueryExpressionBuilder[A] {

    override def const[B](value: B): Aggregation.Of[A, B] =
      new Aggregation.Const[B](value)

    override def toList: Aggregation.Of[A, List[A]] =
      new Aggregation.ToList[A]

    override def toListOf[B](f: A => B): Aggregation.Of[A, List[B]] =
      new Aggregation.ToListOf[A, B](f)

    override def distinct: Aggregation.Of[A, Set[A]] =
      new Aggregation.Distinct[A]

    override def distinctBy[B](f: A => B): Aggregation.Of[A, Set[B]] =
      new Aggregation.DistinctBy[A, B](f)

    override def flatDistinctBy[B](f: A => Iterable[B]): Aggregation.Of[A, Set[B]] =
      new Aggregation.FlatDistinctBy[A, B](f)

    override def count(p: A => Boolean): Aggregation.Of[A, Int] =
      new Aggregation.Count[A](p)

    override def rowNumber: Ranking.Of[A, Int] =
      new Ranking.RowNumber[A]

    override def rank: Ranking.Of[A, Int] =
      new Ranking.Rank[A]

    override def lag[B](f: A => B): Ranking.Of[A, Option[B]] =
      new Ranking.Lag[A, B](f)

    override def min(implicit ev: Ordering[A]): Aggregation.Of[A, A] =
      new Aggregation.Reduce[A](ev.min, opName = "min")

    override def minOf[B](f: A => B)(implicit ev: Ordering[B]): Aggregation.Of[A, B] =
      new Aggregation.ReduceBy[A, B](f, ev.min, opName = "minOf")

    override def max(implicit ev: Ordering[A]): Aggregation.Of[A, A] =
      new Aggregation.Reduce[A](ev.max, opName = "max")

    override def maxOf[B](f: A => B)(implicit ev: Ordering[B]): Aggregation.Of[A, B] =
      new Aggregation.ReduceBy[A, B](f, ev.max, opName = "maxOf")

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
      new Aggregation.Reduce[A](f, opName = "reduce")

    override def reduceBy[B](by: A => B)(f: (B, B) => B): Aggregation.Of[A, B] =
      new Aggregation.ReduceBy[A, B](by, f, "reduceBy")

    override def foldLeft[B](initial: B)(f: (B, A) => B): Aggregation.Of[A, B] =
      new Aggregation.FoldLeft[A, B](initial, f)

    override def foldLeftBy[B, R](by: A => B)(initial: R)(f: (R, B) => R): Aggregation.Of[A, R] =
      new Aggregation.FoldLeftBy[A, B, R](by, initial, f)
  }
}
