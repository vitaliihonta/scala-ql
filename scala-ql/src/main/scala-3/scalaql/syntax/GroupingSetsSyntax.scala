package scalaql.syntax

import scalaql.internal.FatalExceptions

trait GroupingSetsSyntax {
  extension [A](self: A) {
    def rollup: Option[A] = Some(self)
    def cube: Option[A]   = Some(self)
  }

  extension [A](self: Option[A]) {
    def fillna(value: A): A = self.getOrElse(
      throw FatalExceptions.invalidLibraryUsage(s"fillna called outside of groupBy with value=$value")
    )
  }
}
