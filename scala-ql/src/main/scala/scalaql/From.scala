package scalaql

import izumi.reflect.macrortti.LightTypeTag

final class From[A] private[scalaql] (private[scalaql] val inputs: Map[LightTypeTag, Iterable[Any]])
    extends Serializable {

  private[scalaql] def get(tag: LightTypeTag): Iterable[Any] = inputs(tag)

  override def toString: String =
    inputs.keys
      .map(input => s"From[$input]")
      .mkString(" & ")
}

object From {

  private[scalaql] val empty: From[Any] = new From(Map.empty)

  private[scalaql] def singleTag(tag: LightTypeTag, values: Iterable[Any]): From[Any] =
    new From[Any](Map(tag -> values))

  private[scalaql] def single[A: Tag](values: Iterable[A]): From[A] =
    singleTag(Tag[A].tag, values).asInstanceOf[From[A]]

  final implicit class FromSyntax[Self <: From[?]](private val self: Self) extends AnyVal {
    def and[B <: From[?]](that: B): Self & B = new From[Any](self.inputs ++ that.inputs).asInstanceOf[Self & B]

    def &[B <: From[?]](that: B): Self & B = self.and[B](that)
  }
}
