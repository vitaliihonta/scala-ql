package scalaql.utils

import scala.quoted.*

object Scala3MacroUtils {
  def accessorName[A, B](f: Expr[A => B])(using Quotes): String = {
    import quotes.reflect.*
    Expr.betaReduce(f).asTerm.underlying match {
      case Block(List(DefDef(_, _, _, Some(Select(Ident(_), name)))), _) =>
        name
      case _ =>
        throw new IllegalArgumentException(s"Expected a field selector to be passed (as instance.field1), got $f")
    }
  }
}
