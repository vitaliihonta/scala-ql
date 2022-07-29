package scalaql.syntax

import scalaql.*
import scala.quoted.*
import scalaql.utils.Scala3MacroUtils

trait GroupingSyntax {
  extension [In, Out](self: Query[In, Out]) {
    inline def groupBy[A](f: Out => A): GroupedQuery1[In, Out, A] = ???

    inline def groupBy[A, B](
      f1: Out => A,
      f2: Out => B
    ): GroupedQuery2[In, Out, A, B] = ???

    inline def groupByGroupingSets[A, B](
      f1:           Out => A,
      f2:           Out => B
    )(groupingSets: (A, B) => Product
    ): GroupedQuery2[In, Out, Option[A], Option[B]] = ???

    inline def groupBy[A, B, C](
      f1: Out => A,
      f2: Out => B,
      f3: Out => C
    ): GroupedQuery3[In, Out, A, B, C] = ???
  }
}

object GroupingSyntax {
  import Scala3MacroUtils.*

}
