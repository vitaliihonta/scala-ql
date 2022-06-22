package scalaql.utils

import scala.annotation.compileTimeOnly

trait EachSyntax {
  implicit def EachSyntaxIterable[A](self: Iterable[A]): EachSyntaxIterable[A] =
    new EachSyntaxIterable[A](self)

  implicit def EachSyntaxOption[A](self: Option[A]): EachSyntaxOption[A] =
    new EachSyntaxOption[A](self)
}

class EachSyntaxIterable[A](val self: Iterable[A]) extends AnyVal {
  @compileTimeOnly("each can be only used inside builders")
  def each: A = sys.error("")
}

class EachSyntaxOption[A](val self: Option[A]) extends AnyVal {
  @compileTimeOnly("each can be only used inside builders")
  def each: A = sys.error("")
}
