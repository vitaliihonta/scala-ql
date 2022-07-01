package scalaql.syntax

import izumi.reflect.Tag
import scalaql.{From, Query}

final class QueryAliasing[In](private val self: Query[From[In], In]) extends AnyVal {
  def as[U](implicit In: Tag[In], U: Tag[U]): Query[From[In as U], In] =
    new Query.AliasedQuery[In, U](
      In.tag,
      Tag[In as U].tag,
      Tag[U].tag
    )
}

final class FromAliasing[A](private val self: From[A]) extends AnyVal {
  def as[U](implicit A: Tag[A], U: Tag[U]): From[A as U] = {
    val restInputs = self.inputs.filterNot { case (tag, _) => tag == A.tag }
    new From[A as U](
      restInputs.updated(Tag[A as U].tag, self.get(A.tag))
    )
  }
}
