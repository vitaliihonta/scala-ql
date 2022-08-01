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
  private val Fillna = "fillna"

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

  def getGroupingMeta[Out: Type, A](f: Expr[Out => A])(using q: Quotes, A: Type[A]): GroupingMeta[Expr, Out, A] = {
    import q.reflect.*

    val (chainTree, groupFillOverrideOpt, defaultFillOverrideOpt) = Expr.betaReduce(f).asTerm.underlying match {
      case Block(List(DefDef(_, List(param), tpt, Some(Select(qual, Rollup | Cube)))), _) =>
        println(s"GroupingMeta param=$param qual=$qual")
        ???
      case Block(
            List(
              DefDef(_,
                     List(param),
                     tpt,
                     Some(
                       Apply(
                         Apply(
                           TypeApply(
                             Ident(Fillna),
                             _
                           ),
                           List(
                             body @ Apply(
                               TypeApply(Ident(Rollup | Cube), _),
                               _
                             )
                           )
                         ),
                         List(fillment)
                       )
                     )
              )
            ),
            _
          ) =>
        println(s"GroupingMeta fillna param=$param cls=${body.getClass} body=$body fillment=$fillment")
        (body, Some('{ (fill: Any) => fill }), Some(fillment.asExpr))
      case other => (other, None, None)
    }

    val callChain: List[Call] = Scala3MacroUtils.accessorCallPathTerm(chainTree, ignoreUnmatched)
    println(s"Call chain=$callChain")
    callChain.lastOption match {
      case Some(Call(term @ (Rollup | Cube), _)) =>
        val (theIn, orderingOpt) = A match {
          case '[Option[t]] =>
            Type.of[t] -> Expr.summon[Ordering[t]]
          case _ => A -> Expr.summon[Ordering[A]]
        }

        println(s"The in is ${Type.show(using theIn)}")

        val ordering = orderingOpt.getOrElse(
          sys.error(
            s"Implicit ordering for grouping key is required in case of rollup/cube. Not found Ordering of ${Type.show(using theIn)}"
          )
        )

        val group = groupFillOverrideOpt
          .map(_ => f)
          .getOrElse('{ (out: Out) => $f(out).asInstanceOf[Option[Any]].get })

        val kind = if (term == Cube) KindCube else KindRollup

        val groupFillment = groupFillOverrideOpt
          .getOrElse('{ (a: Any) => Some(a) })

        val defaultFillments = defaultFillOverrideOpt
          .orElse(Some[Expr[Any]]('{ None }))

        GroupingMeta[Expr, Out, A](
          groupFuncBody = group.asInstanceOf[Expr[Out => A]],
          kind = kind,
          ordering = '{ $ordering.asInstanceOf[Ordering[Any]] },
          groupFillments = groupFillment,
          defaultFillments = defaultFillments
        )
      case _ =>
        val group = f
        val kind  = KindSimple
        val ordering = Expr
          .summon[Ordering[A]]
          .getOrElse('{ NaturalOrdering[A] })

        val groupFillment   = '{ (a: Any) => a }
        val defaultFillment = None

        GroupingMeta[Expr, Out, A](
          groupFuncBody = group,
          kind = kind,
          ordering = '{ $ordering.asInstanceOf[Ordering[Any]] },
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
            values = ${ Expr(sets) },
            orderings = ${ Expr.ofList(orderings) },
            groupFillments = ${ Expr.ofList(groupFills) }.toMap,
            defaultFillments = ${ Expr.ofList(defaultFills) }.toMap
          )
        }
    )
  }
}
