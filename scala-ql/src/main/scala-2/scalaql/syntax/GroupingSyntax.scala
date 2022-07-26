package scalaql.syntax

import scalaql.Query
import scala.language.implicitConversions

trait GroupingSyntax {

  final implicit def GroupBySyntax[In, Out](self: Query[In, Out]): GroupBySyntax[In, Out] =
    new GroupBySyntax[In, Out](self)
}
