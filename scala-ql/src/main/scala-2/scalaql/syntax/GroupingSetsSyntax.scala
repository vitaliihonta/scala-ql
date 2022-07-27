package scalaql.syntax

import scalaql.internal.FatalExceptions
import scala.language.implicitConversions

trait GroupingSetsSyntax {
  implicit def GroupingSetsOps[A](self: A): GroupingSetsOps[A] =
    new GroupingSetsOps[A](self)

  implicit def GroupingSetsFillNAOps[A](self: Option[A]): GroupingSetsFillNAOps[A] =
    new GroupingSetsFillNAOps[A](self)
}

final class GroupingSetsOps[A](val self: A) extends AnyVal {
  def rollup: Option[A] = Some(self)
  def cube: Option[A]   = Some(self)
}

final class GroupingSetsFillNAOps[A](val self: Option[A]) extends AnyVal {
  def fillna(value: A): A = self.getOrElse(
    throw FatalExceptions.invalidLibraryUsage(s"fillna called outside of groupBy with value=$value")
  )
}
