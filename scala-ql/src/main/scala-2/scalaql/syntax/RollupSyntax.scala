package scalaql.syntax

import scala.annotation.compileTimeOnly
import scala.language.implicitConversions

trait RollupSyntax {
  implicit def RollupOps[A](self: A): RollupOps[A] =
    new RollupOps[A](self)

  implicit def RollupOptionOps[A](self: Option[A]): RollupOptionOps[A] =
    new RollupOptionOps[A](self)
}

final class RollupOps[A](val self: A) extends AnyVal {
  def rollup: Option[A] = Some(self)
}

final class RollupOptionOps[A](val self: Option[A]) extends AnyVal {
  @compileTimeOnly("fillna should be used only inside groupBy, after rollup")
  def fillna(value: A): A = ???
}
