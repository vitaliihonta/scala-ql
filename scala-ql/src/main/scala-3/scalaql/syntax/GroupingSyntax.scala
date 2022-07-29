package scalaql.syntax

import scalaql.*
import scala.quoted.*
import scalaql.utils.Scala3MacroUtils
import scalaql.internal.GroupByMacroSharedUtils.*

trait GroupingSyntax {
  extension [In, Out](self: Query[In, Out]) {
    inline def groupBy[A](f: Out => A): GroupedQuery1[In, Out, A] =
      ${ GroupingSyntax.groupByImpl[In, Out, A]('self, 'f) }

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

  def groupByImpl[In: Type, Out: Type, A: Type](
    self:    Expr[Query[In, Out]],
    f:       Expr[Out => A]
  )(using q: Quotes
  ): Expr[GroupedQuery1[In, Out, A]] = {
    val groupingMeta = getGroupingMeta[Out, A](f)
    ???
  }

  def getGroupingMeta[Out: Type, A: Type](f: Expr[Out => A])(using q: Quotes): GroupingMeta[q.reflect.Tree] = {
    import q.reflect.*

    Expr.betaReduce(f).asTerm.underlying match {
      case Block(List(DefDef(_, List(param), tpt, Some(Select(qual, sym)))), _) =>
        println(s"GroupingMeta param=$param qual=$qual sym=$sym")
        ???
      case Block(List(DefDef(_, List(param), tpt, Some(Apply(fun, args)))), _) =>
        println(s"GroupingMeta param=$param cls=${fun.getClass} fun=$fun size=${args.size} args=$args")
        ???
    }
  }
}
