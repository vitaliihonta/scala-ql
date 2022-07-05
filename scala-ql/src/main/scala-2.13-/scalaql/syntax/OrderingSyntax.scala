package scalaql.syntax

import scalaql.Query
import scala.language.implicitConversions

trait OrderingSyntax {
  implicit def OrderingAscDescSyntax[A](self: A): OrderingAscDescSyntax[A] =
    new OrderingAscDescSyntax[A](self)

  final implicit def OrderBySyntax[In, Out](self: Query[In, Out]): OrderBySyntax[In, Out] =
    new OrderBySyntax[In, Out](self)
}

final class OrderingAscDescSyntax[A](val self: A) extends AnyVal {
  def asc: A = self

  def desc: A = self
}
