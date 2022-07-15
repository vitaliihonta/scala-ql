package scalaql.syntax

trait EachSyntax {
  extension [A](self: Iterable[A]) {
    def each: A = sys.error("each can be only used inside builders")
  }
  extension [A](self: Option[A]) {
    def each: A = sys.error("each can be only used inside builders")
  }
}
