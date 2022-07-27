package scalaql.syntax

import scalaql.internal.FatalExceptions
import scalaql.internalApi
import scala.language.implicitConversions

trait GroupingSetsSyntax {
  implicit def GroupingSetsOps[A](self: A): GroupingSetsOps[A] =
    new GroupingSetsOps[A](self)

  implicit def GroupingSetsFillNAOps[A](self: Option[A]): GroupingSetsFillNAOps[A] =
    new GroupingSetsFillNAOps[A](self)
}

final class GroupingSetsOps[A](@internalApi val __scalaql_self: A) extends AnyVal {
  def rollup: Option[A] = Some(__scalaql_self)
  def cube: Option[A]   = Some(__scalaql_self)
}

final class GroupingSetsFillNAOps[A](@internalApi val __scalaql_self: Option[A]) extends AnyVal {
  def fillna(value: A): A = __scalaql_self.getOrElse(
    throw FatalExceptions.invalidLibraryUsage(s"fillna called outside of groupBy with value=$value")
  )
}
