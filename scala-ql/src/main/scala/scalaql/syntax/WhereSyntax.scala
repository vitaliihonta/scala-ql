package scalaql.syntax

class WhereSyntax[A](private val self: A) extends AnyVal {

  def isIn(first: A, second: A, rest: A*): Boolean = {
    val values = first +: second +: rest
    values.contains(self)
  }

  def isInCollection(values: Set[A]): Boolean = values.contains(self)
  def isInCollection(values: Seq[A]): Boolean = values.contains(self)
}
