package scalaql

import izumi.reflect.macrortti.LightTypeTag

/**
 * Special container which is holds query input values.
 * It's used to properly represent a result of `join`s, `flatMap`s, etc.
 *
 * Example:
 * {{{
 *   val joined: Query[From[Employee] with From[Company], (Employee, Company)] =
 *     select[Employee]
 *       .join(select[Company])
 *       .on(_.companyId == _.id)
 * }}}
 *
 * `scalaql` uses `From` to get appropriate input values based on their type.
 *
 * Based on original idea of ZIO authors (zio.Has)
 * @see [[https://github.com/zio/zio/blob/dd114ff4b9ab8c10d3e5bb6f9ce4d04e18754d8e/core/shared/src/main/scala/zio/Has.scala#L34 zio.Has]]
 * */
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

    /**
     * Merges this input with the specified input.
     *
     * @tparam B more input types
     * @param that more inputs
     * @return merges `this` and `that` inputs
     * */
    def and[B <: From[?]](that: B): Self & B = new From[Any](self.inputs ++ that.inputs).asInstanceOf[Self & B]

    /** 
     * Symbolic alias for `and`
     * 
     * @see [[and]]
     * */
    def &[B <: From[?]](that: B): Self & B = self.and[B](that)
  }
}
