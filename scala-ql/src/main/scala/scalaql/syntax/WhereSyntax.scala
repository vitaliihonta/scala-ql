package scalaql.syntax

final class WhereSyntax[A](private val self: A) extends AnyVal {

  /**
   * Check whenever `this` value is one of the specified values.
   * 
   * Example:
   * {{{
   *   select[Employee]
   *     .where(_.profession isIn (Programmer, Manager))
   * }}}
   * */
  def isIn(first: A, second: A, rest: A*): Boolean = {
    val values = first +: second +: rest
    values.contains(self)
  }

  /**
   * Check whenever `this` value is one of the specified values.
   *
   * Example:
   * {{{
   *   select[Employee]
   *     .where(_.profession isInCollection Set(Programmer, Manager))
   * }}}
   * */
  def isInCollection(values: Set[A]): Boolean = values.contains(self)

  /**
   * Check whenever `this` value is one of the specified values.
   *
   * Example:
   * {{{
   *   select[Employee]
   *     .where(_.profession isInCollection List(Programmer, Manager))
   * }}}
   * */
  def isInCollection(values: Seq[A]): Boolean = values.contains(self)
}
