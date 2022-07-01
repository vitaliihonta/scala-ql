package scalaql.syntax

import izumi.reflect.Tag
import scalaql.From
import scalaql.Query

final class SelectDsl private[scalaql] () {
  def apply[A: Tag]: Query[From[A], A] = new Query.FromQuery[A]

  def from[A: Tag](values: Iterable[A]): Query[Any, A] = new Query.Const[A](values)
}
