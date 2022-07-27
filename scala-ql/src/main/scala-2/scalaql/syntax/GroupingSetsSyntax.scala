package scalaql.syntax

import scala.annotation.compileTimeOnly
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
  @compileTimeOnly("fillna should be used only inside groupBy, after rollup or cube")
  def fillna(value: A): A = ???
}
