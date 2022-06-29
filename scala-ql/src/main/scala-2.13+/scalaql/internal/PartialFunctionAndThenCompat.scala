package scalaql.internal

object PartialFunctionAndThenCompat {
  def andThen[A, B, C](pf1: PartialFunction[A, B], pf2: PartialFunction[B, C]): PartialFunction[A, C] =
    pf1 andThen pf2
}
