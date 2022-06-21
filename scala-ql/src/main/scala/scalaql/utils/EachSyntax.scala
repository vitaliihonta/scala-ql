package scalaql.utils

import scala.annotation.compileTimeOnly

trait EachSyntax {
  implicit class EachSyntaxImpl[A](val self: Iterable[A]) {
    @compileTimeOnly("each can be only used inside builders")
    def each: A = sys.error("")
  }
}
