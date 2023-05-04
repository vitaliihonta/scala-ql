package scalaql.internal

import scalaql.utils.{MathUtils, TupleFlatten}
import scalaql.{Aggregation, forbiddenInheritance}
import spire.algebra.{AdditiveMonoid, Field, MultiplicativeMonoid}
import spire.math.Fractional
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

@forbiddenInheritance
trait AggregationFunctions {

  trait AggregateWithSubtotals[A] extends Aggregation[A] {
    protected def updateImpl(acc: Acc, value: A): Acc

    override def update(acc: Acc, value: A, isSubtotal: Boolean): Acc =
      updateImpl(acc, value)
  }

  final class AggregateIgnoreSubtotals[A, Acc0, Out0](base: Aggregation.Aux[A, Acc0, Out0]) extends Aggregation[A] {
    override type Out = Out0
    override type Acc = Acc0

    override def init(): Acc0 = base.init()

    override def update(acc: Acc0, value: A, isSubtotal: Boolean): Acc0 =
      if (isSubtotal) acc
      else base.update(acc, value, isSubtotal)

    override def result(acc: Acc0): Out0 = base.result(acc)
  }

  final class Const[A](value: A) extends AggregateWithSubtotals[Any] {

    override type Out = A
    override type Acc = Unit

    override def init(): Unit                            = ()
    override def updateImpl(acc: Unit, value: Any): Unit = ()
    override def result(acc: Unit): A                    = value
  }

  final class Contramapped[A0, A, Acc0, B](base: Aggregation.Aux[A, Acc0, B], f: A0 => A) extends Aggregation[A0] {

    override type Out = B
    override type Acc = Acc0

    override def init(): Acc                                             = base.init()
    override def update(acc: Acc0, value: A0, isSubtotal: Boolean): Acc0 = base.update(acc, f(value), isSubtotal)
    override def result(acc: Acc0): B                                    = base.result(acc)
  }

  final class Mapped[A, Out0, Acc0, B](base: Aggregation.Aux[A, Acc0, Out0], f: Out0 => B) extends Aggregation[A] {

    override type Out = B
    override type Acc = Acc0

    override def init(): Acc0                                           = base.init()
    override def update(acc: Acc0, value: A, isSubtotal: Boolean): Acc0 = base.update(acc, value, isSubtotal)
    override def result(acc: Acc0): B                                   = f(base.result(acc))
  }

  final class Chained[A, Out0, Out1, Acc0, Acc1, U](
    f:      Aggregation.Aux[A, Acc0, Out0],
    g:      Aggregation.Aux[A, Acc1, Out1]
  )(tupled: TupleFlatten.Of[(Out0, Out1), U])
      extends Aggregation[A] {

    override type Out = U
    override type Acc = (Acc0, Acc1)

    override def init(): (Acc0, Acc1) = f.init() -> g.init()
    override def update(acc: (Acc0, Acc1), value: A, isSubtotal: Boolean): (Acc0, Acc1) = {
      val (acc0, acc1) = acc
      f.update(acc0, value, isSubtotal) -> g.update(acc1, value, isSubtotal)
    }

    override def result(acc: (Acc0, Acc1)): U = {
      val (acc0, acc1) = acc
      tupled.apply(f.result(acc0) -> g.result(acc1))
    }
  }

  trait Unoptimized[A] extends AggregateWithSubtotals[A] {
    protected type By
    protected def by(value: A): By
    protected def apply(xs: Iterable[By]): Out

    override final type Acc = mutable.ListBuffer[By]
    override final def init(): mutable.ListBuffer[By]                            = mutable.ListBuffer.empty[By]
    override final def updateImpl(acc: ListBuffer[By], value: A): ListBuffer[By] = acc += by(value)
    override final def result(acc: ListBuffer[By]): Out                          = apply(acc)
  }

  final class ToList[A] extends AggregateWithSubtotals[A] {
    override type Out = List[A]
    override type Acc = mutable.Builder[A, List[A]]

    override def init(): Acc = List.newBuilder[A]

    override def updateImpl(acc: mutable.Builder[A, List[A]], value: A): mutable.Builder[A, List[A]] =
      acc += value

    override def result(acc: mutable.Builder[A, List[A]]): List[A] = acc.result()
  }

  final class ToListOf[A, B](f: A => B) extends AggregateWithSubtotals[A] {
    override type Out = List[B]

    override type Acc = mutable.Builder[B, List[B]]

    override def init(): Acc = List.newBuilder[B]

    override def updateImpl(acc: mutable.Builder[B, List[B]], value: A): mutable.Builder[B, List[B]] =
      acc += f(value)

    override def result(acc: mutable.Builder[B, List[B]]): List[B] = acc.result()
  }

  final class Distinct[A] extends AggregateWithSubtotals[A] {
    override type Out = Set[A]
    override type Acc = mutable.Builder[A, Set[A]]

    override def init(): Acc = Set.newBuilder[A]

    override def updateImpl(acc: mutable.Builder[A, Set[A]], value: A): mutable.Builder[A, Set[A]] =
      acc += value

    override def result(acc: mutable.Builder[A, Set[A]]): Set[A] = acc.result()
  }

  final class DistinctBy[A, B](f: A => B) extends AggregateWithSubtotals[A] {
    override type Out = Set[B]
    override type Acc = mutable.Builder[B, Set[B]]

    override def init(): Acc = Set.newBuilder[B]

    override def updateImpl(acc: mutable.Builder[B, Set[B]], value: A): mutable.Builder[B, Set[B]] =
      acc += f(value)

    override def result(acc: mutable.Builder[B, Set[B]]): Set[B] = acc.result()
  }

  final class FlatDistinctBy[A, B](f: A => Iterable[B]) extends AggregateWithSubtotals[A] {
    override type Out = Set[B]
    override type Acc = mutable.Builder[B, Set[B]]

    override def init(): Acc = Set.newBuilder[B]

    override def updateImpl(acc: mutable.Builder[B, Set[B]], value: A): mutable.Builder[B, Set[B]] =
      acc ++= f(value)

    override def result(acc: mutable.Builder[B, Set[B]]): Set[B] = acc.result()
  }

  final class Sum[A](ev: AdditiveMonoid[A]) extends AggregateWithSubtotals[A] {
    override type Out = A
    override type Acc = A

    override def init(): A                       = ev.zero
    override def updateImpl(acc: A, value: A): A = ev.plus(acc, value)
    override def result(acc: A): A               = acc
  }

  final class SumBy[A, B](f: A => B, ev: AdditiveMonoid[B]) extends AggregateWithSubtotals[A] {
    override type Out = B
    override type Acc = B

    override def init(): B                       = ev.zero
    override def updateImpl(acc: B, value: A): B = ev.plus(acc, f(value))
    override def result(acc: B): B               = acc
  }

  final class Count[A](p: A => Boolean) extends AggregateWithSubtotals[A] {
    override type Out = Int
    override type Acc = Int

    override def init(): Int                         = 0
    override def updateImpl(acc: Int, value: A): Int = acc + 1
    override def result(acc: Int): Int               = acc
  }

  final class Product[A](ev: MultiplicativeMonoid[A]) extends AggregateWithSubtotals[A] {
    override type Out = A
    override type Acc = A

    override def init(): A                       = ev.one
    override def updateImpl(acc: A, value: A): A = ev.times(acc, value)
    override def result(acc: A): A               = acc
  }

  final class ProductBy[A, B](f: A => B, ev: MultiplicativeMonoid[B]) extends AggregateWithSubtotals[A] {

    override type Out = B
    override type Acc = B

    override def init(): B                       = ev.one
    override def updateImpl(acc: B, value: A): B = ev.times(acc, f(value))
    override def result(acc: B): B               = acc
  }

  final class Avg[A](ev: Field[A]) extends AggregateWithSubtotals[A] {

    override type Out = A
    override type Acc = (Int, A)

    override def init(): (Int, A) = (0, ev.zero)

    override def updateImpl(acc: (Int, A), value: A): (Int, A) = {
      val (count, sum) = acc
      (count + 1, ev.plus(sum, value))
    }

    override def result(acc: (Int, A)): A = {
      val (count, sum) = acc
      ev.div(sum, ev.fromInt(count))
    }
  }

  final class AvgBy[A, B](f: A => B, ev: Field[B]) extends AggregateWithSubtotals[A] {

    override type Out = B
    override type Acc = (Int, B)

    override def init(): (Int, B) = (0, ev.zero)

    override def updateImpl(acc: (Int, B), value: A): (Int, B) = {
      val (count, sum) = acc
      (count + 1, ev.plus(sum, f(value)))
    }

    override def result(acc: (Int, B)): B = {
      val (count, sum) = acc
      ev.div(sum, ev.fromInt(count))
    }
  }

  final class Std[A](ev: Fractional[A]) extends Unoptimized[A] {
    override type Out = A

    override protected type By = A
    override protected def by(value: A): A = value

    override def apply(xs: Iterable[A]): A =
      MathUtils.std[A](xs)(ev).value
  }

  final class StdBy[A, B](f: A => B, ev: Fractional[B]) extends Unoptimized[A] {
    override type Out = B

    override protected type By = B
    override protected def by(value: A): B = f(value)

    override def apply(xs: Iterable[By]): B =
      MathUtils.std[B](xs)(ev).value
  }

  final class Reduce[A](op: (A, A) => A, opName: String) extends AggregateWithSubtotals[A] {
    override type Out = A
    override type Acc = A

    override def init(): A = null.asInstanceOf[A]
    override def updateImpl(acc: A, value: A): A =
      if (acc == null) value
      else op(acc, value)

    override def result(acc: A): A =
      if (acc == null) throw FatalExceptions.emptyGroupByResult(s"Cannot calculate $opName")
      else acc
  }

  final class ReduceBy[A, B](by: A => B, op: (B, B) => B, opName: String) extends AggregateWithSubtotals[A] {
    override type Out = B
    override type Acc = B

    override def init(): B = null.asInstanceOf[B]
    override def updateImpl(acc: B, value: A): B =
      if (acc == null) by(value)
      else op(acc, by(value))

    override def result(acc: B): B =
      if (acc == null) throw FatalExceptions.emptyGroupByResult(s"Cannot calculate $opName")
      else acc
  }

  final class FoldLeft[A, B](initial: B, f: (B, A) => B) extends AggregateWithSubtotals[A] {
    override type Out = B
    override type Acc = B

    override def init(): B                       = initial
    override def updateImpl(acc: B, value: A): B = f(acc, value)
    override def result(acc: B): B               = acc
  }

  final class FoldLeftBy[A, B, R](by: A => B, initial: R, f: (R, B) => R) extends AggregateWithSubtotals[A] {
    override type Out = R
    override type Acc = R

    override def init(): R                       = initial
    override def updateImpl(acc: R, value: A): R = f(acc, by(value))
    override def result(acc: R): R               = acc
  }

//  final class Report1[A, B, U1](
//    view1:  QueryExpressionBuilder[A]
//  )(group1: A => B,
//    agg1:   (B, QueryExpressionBuilder[A]) => Aggregation.Of[A, U1])
//      extends Aggregation[A] {
//
//    override type Out = List[U1]
//
//    override def apply(xs: Iterable[A]): List[U1] =
//      xs.groupBy(group1)
//        .view
//        .map { case (sliceB, bs) =>
//          agg1(sliceB, view1).apply(bs)
//        }
//        .toList
//  }
//
//  final class Report2[A, B, C, U1, U2](
//    view1:  QueryExpressionBuilder[A],
//    view2:  QueryExpressionBuilder[U1]
//  )(group1: A => B,
//    group2: A => C,
//    agg1:   (B, C, QueryExpressionBuilder[A]) => Aggregation.Of[A, U1],
//    agg2:   (B, QueryExpressionBuilder[U1]) => Aggregation.Of[U1, U2])
//      extends Aggregation[A] {
//
//    override type Out = List[U2]
//
//    override def apply(xs: Iterable[A]): List[U2] =
//      xs.groupBy(group1)
//        .view
//        .map { case (sliceB, bs) =>
//          val merged1 = bs.groupBy(group2).view.map { case (sliceC, cs) =>
//            agg1(sliceB, sliceC, view1).apply(cs)
//          }
//          agg2(sliceB, view2).apply(merged1.toList)
//        }
//        .toList
//  }
}
