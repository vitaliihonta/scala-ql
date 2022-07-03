package scalaql.internal

import scalaql.{Ranking, forbiddenInheritance}
import scala.collection.mutable

@forbiddenInheritance
trait RankingFunctions {

  final class RowNumber[A] extends Ranking[A] {
    override type Out = Int

    override def apply(order: Ordering[A], values: Iterable[A]): Iterable[(A, Int)] =
      values.zipWithIndex.map { case (v, idx) => v -> (idx + 1) }
  }

  final class Rank[A] extends Ranking[A] {
    override type Out = Int

    override def apply(order: Ordering[A], values: Iterable[A]): Iterable[(A, Int)] = {
      val iter = values.iterator

      var currentRank = 1
      var streak      = 0
      var prev        = iter.next()

      val buffer = mutable.ListBuffer.empty[(A, Int)]
      buffer += (prev -> currentRank)

      while (iter.hasNext) {
        val current = iter.next()
        if (order.equiv(prev, current)) {
          streak += 1
        } else {
          currentRank += 1 + streak
          streak = 0
        }
        buffer += (current -> currentRank)
        prev = current
      }

      buffer.toList
    }
  }

  final class Lag[A, B](f: A => B) extends Ranking[A] {
    override type Out = Option[B]

    override def apply(order: Ordering[A], values: Iterable[A]): Iterable[(A, Option[B])] = {
      val iter = values.iterator

      var prev = iter.next()

      val buffer = mutable.ListBuffer.empty[(A, Option[B])]
      buffer += (prev -> None)

      while (iter.hasNext) {
        val current = iter.next()
        buffer += (current -> Some(f(prev)))
        prev = current
      }

      buffer.toList
    }
  }

  //  final class Contramapped[A, A0, Out0](base: Ranking.Of[A, Out0], f: A0 => A) extends Ranking[A0] {
  //    override type Ranked = Out0
  //
  //    override def apply(values: Iterable[A0]): Iterable[(A0, Out0)] =
  //      base
  //  }

  final class Mapped[A, Out0, B](base: Ranking.Of[A, Out0], f: Out0 => B) extends Ranking[A] {
    override type Out = B

    override def apply(order: Ordering[A], xs: Iterable[A]): Iterable[(A, B)] =
      base(order, xs).map { case (value, rank) =>
        value -> f(rank)
      }
  }
}
