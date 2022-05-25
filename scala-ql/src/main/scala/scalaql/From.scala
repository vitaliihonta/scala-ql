package scalaql

import izumi.reflect.Tag
import izumi.reflect.macrortti.LightTypeTag

final class From[A] private (private[scalaql] val inputs: Map[LightTypeTag, Iterable[Any]]) extends Serializable {
  private[scalaql] def get(tag: LightTypeTag): Iterable[Any] = inputs(tag)
}

object From {

  private[scalaql] val empty: From[Any] = new From(Map.empty)

  private[scalaql] def singleTag(tag: LightTypeTag, values: Iterable[Any]): From[Any] =
    new From[Any](Map(tag -> values))

  private[scalaql] def single[A: Tag](values: Iterable[A]): From[A] =
    singleTag(Tag[A].tag, values).asInstanceOf[From[A]]

  final implicit class FromSyntax[Self <: From[_]](private val self: Self) extends AnyVal {
    def and[B <: From[_]](that: B): Self with B = new From[Any](self.inputs ++ that.inputs).asInstanceOf[Self with B]

    def &[B <: From[_]](that: B): Self with B = self.and[B](that)
  }
}
