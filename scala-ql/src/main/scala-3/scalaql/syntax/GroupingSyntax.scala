package scalaql.syntax

import scalaql.*

import scala.quoted.*
import scalaql.utils.Scala3MacroUtils
import scalaql.internal.GroupByMacroSharedUtils.*
import scalaql.internal.{GroupByMacroSharedUtils, NaturalOrdering}

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

  private val Rollup = "rollup"
  private val Cube   = "cube"

  def groupByImpl[In: Type, Out: Type, A: Type](
    self:    Expr[Query[In, Out]],
    f:       Expr[Out => A]
  )(using q: Quotes
  ): Expr[GroupedQuery1[In, Out, A]] = {
    val meta         = getGroupingMeta[Out, A](f)
    val groupingSets = buildGroupingSets(List(meta.widen))

    '{
      new GroupedQuery1[In, Out, A]($self, ${ meta.groupFuncBody }, $groupingSets)
    }
  }

  def getGroupingMeta[Out: Type, A: Type](f: Expr[Out => A])(using q: Quotes): GroupingMeta[Expr, Out, A] = {
    import q.reflect.*

    val (chainTree, groupFillOverrideOpt, defaultFillOverrideOpt) = Expr.betaReduce(f).asTerm.underlying match {
      case Block(List(DefDef(_, List(param), tpt, Some(Select(qual, sym)))), _) =>
        println(s"GroupingMeta param=$param qual=$qual sym=$sym")
        ???
      case Block(List(DefDef(_, List(param), tpt, Some(Apply(fun, args)))), _) =>
        println(s"GroupingMeta param=$param cls=${fun.getClass} fun=$fun size=${args.size} args=$args")
        ???
      case other => (other, None, None)
    }

    val callChain: List[Call] = Scala3MacroUtils.accessorCallPathTerm(chainTree, ignoreUnmatched)
    callChain.lastOption match {
      case Some(Call(term @ (Rollup | Cube), _)) =>
        ???
      case _ =>
        val group = f
        val kind  = KindSimple
        val ordering = summonOption[Ordering[A]]
          .getOrElse(NaturalOrdering[A])

        val groupFillment   = '{ (a: Any) => a }
        val defaultFillment = None

        GroupingMeta[Expr, Out, A](
          groupFuncBody = group,
          kind = kind,
          ordering = ordering, // TODO: fix
          groupFillments = groupFillment,
          defaultFillments = defaultFillment
        )
    }
  }

  private def buildGroupingSets(
    using q: Quotes
  )(metas:   List[GroupingMeta[Expr, Any, Any]]
  ): Expr[Query.GroupingSetsDescription] = {
    import q.reflect.*
    GroupByMacroSharedUtils.buildGroupingSets[Expr](metas)(
      e => throw new RuntimeException(e),
      toGroupFills = (m, idx) => '{ ${ Expr(idx) } -> ${ m.groupFillments } },
      toDefaultFills = (df, idx) => '{ ${ Expr(idx) } -> $df },
      buildTree = (sets, orderings, groupFills, defaultFills) =>
        '{
          Query.GroupingSetsDescription(
            values = sets,
            orderings = ${ Expr.ofList(orderings) },
            groupFillments = ${ Expr.ofList(groupFills) }.toMap,
            defaultFillments = ${ Expr.ofList(defaultFills) }.toMap
          )
        }
    )
  }
}
