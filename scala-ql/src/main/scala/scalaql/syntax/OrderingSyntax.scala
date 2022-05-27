package scalaql.syntax

import algebra.Order

trait OrderingSyntax {
  def desc[A: Order]: Order[A] = Order.reverse(Order[A])
  def asc[A: Order]: Order[A]  = Order[A]
}
