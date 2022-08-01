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
    ): GroupedQuery2[In, Out, A, B] =
      ${ GroupingSyntax.groupBy2Impl[In, Out, A, B]('self, 'f1, 'f2) }

    // TODO: add arity 3
    inline def groupByGroupingSets[A, B](
      f1:                  Out => A,
      f2:                  Out => B
    )(inline groupingSets: (A, B) => Tuple
    ): GroupedQuery2[In, Out, Option[A], Option[B]] =
      ${ GroupingSyntax.groupBy2GroupingSetsImpl[In, Out, A, B]('self, 'f1, 'f2, 'groupingSets) }

    inline def groupBy[A, B, C](
      f1: Out => A,
      f2: Out => B,
      f3: Out => C
    ): GroupedQuery3[In, Out, A, B, C] =
      ${ GroupingSyntax.groupBy3Impl[In, Out, A, B, C]('self, 'f1, 'f2, 'f3) }
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

  def groupBy2Impl[In: Type, Out: Type, A: Type, B: Type](
    self:    Expr[Query[In, Out]],
    f1:      Expr[Out => A],
    f2:      Expr[Out => B]
  )(using q: Quotes
  ): Expr[GroupedQuery2[In, Out, A, B]] = {
    val meta1        = getGroupingMeta[Out, A](f1)
    val meta2        = getGroupingMeta[Out, B](f2)
    val groupingSets = buildGroupingSets(List(meta1.widen, meta2.widen))

    '{
      new GroupedQuery2[In, Out, A, B](
        $self,
        ${ meta1.groupFuncBody },
        ${ meta2.groupFuncBody },
        $groupingSets
      )
    }
  }

  def groupBy3Impl[In: Type, Out: Type, A: Type, B: Type, C: Type](
    self:    Expr[Query[In, Out]],
    f1:      Expr[Out => A],
    f2:      Expr[Out => B],
    f3:      Expr[Out => C]
  )(using q: Quotes
  ): Expr[GroupedQuery3[In, Out, A, B, C]] = {
    val meta1        = getGroupingMeta[Out, A](f1)
    val meta2        = getGroupingMeta[Out, B](f2)
    val meta3        = getGroupingMeta[Out, C](f3)
    val groupingSets = buildGroupingSets(List(meta1.widen, meta2.widen, meta3.widen))

    '{
      new GroupedQuery3[In, Out, A, B, C](
        $self,
        ${ meta1.groupFuncBody },
        ${ meta2.groupFuncBody },
        ${ meta3.groupFuncBody },
        $groupingSets
      )
    }
  }

  def groupBy2GroupingSetsImpl[In: Type, Out: Type, A: Type, B: Type](
    self:         Expr[Query[In, Out]],
    f1:           Expr[Out => A],
    f2:           Expr[Out => B],
    groupingSets: Expr[(A, B) => Tuple]
  )(using q:      Quotes
  ): Expr[GroupedQuery2[In, Out, Option[A], Option[B]]] = {
    import q.reflect.*

//    println(s"GroupingSets: ${groupingSets.asTerm.show}")

    val meta1 = getGroupingMeta[Out, A](f1)
    val meta2 = getGroupingMeta[Out, B](f2)
    val groupingSetsTree = extractGroupingSets(
      Expr.betaReduce(groupingSets).asTerm.underlying,
      List(meta1.widen, meta2.widen)
    )

    '{
      new GroupedQuery2[In, Out, Option[A], Option[B]](
        $self,
        ${ meta1.groupFuncBody },
        ${ meta2.groupFuncBody },
        $groupingSetsTree
      )
    }
  }

  def getGroupingMeta[Out: Type, A](f: Expr[Out => A])(using q: Quotes, A: Type[A]): GroupingMeta[Expr, Out, A] = {
    import q.reflect.*

    val (chainTree, groupFillOverrideOpt, defaultFillOverrideOpt) = Expr.betaReduce(f).asTerm.underlying match {
      case Block(List(DefDef(_, List(param), tpt, Some(Select(qual, Rollup | Cube)))), _) =>
//        println(s"GroupingMeta param=$param qual=$qual")
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
//        println(s"GroupingMeta fillna param=$param cls=${body.getClass} body=$body fillment=$fillment")
        (body, Some('{ (fill: Any) => fill }), Some(fillment.asExpr))
      case other => (other, None, None)
    }

    val callChain: List[Call] = Scala3MacroUtils.accessorCallPathTerm(chainTree, ignoreUnmatched)
//    println(s"Call chain=$callChain")
    callChain.lastOption match {
      case Some(Call(term @ (Rollup | Cube), _)) =>
        val (theIn, orderingOpt) = A match {
          case '[Option[t]] =>
            Type.of[t] -> Expr.summon[Ordering[t]]
          case _ => A -> Expr.summon[Ordering[A]]
        }

//        println(s"The in is ${Type.show(using theIn)}")

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

  private def extractGroupingSets(
    using q:     Quotes
  )(groupingSet: q.reflect.Term,
    metas:       List[GroupingMeta[Expr, Any, Any]]
  ): Expr[Query.GroupingSetsDescription] = {
    import q.reflect.*

    val allowedApplies = Set(
      TypeRepr.of[Tuple1.type],
      TypeRepr.of[Tuple2.type],
      TypeRepr.of[Tuple3.type],
      TypeRepr.of[Tuple4.type],
      TypeRepr.of[Tuple5.type],
      TypeRepr.of[Tuple6.type],
      TypeRepr.of[Tuple7.type],
      TypeRepr.of[Tuple8.type]
    )

//    println(s"GrSets: ${groupingSet.getClass}")

    def toGroupings(tree: Term, names: Map[String, Int]): List[Int] =
      //      println(tree)
      //      println(tree.getClass)
      tree match {
        case Literal(c: Constant) if c.value == () =>
          Nil
        case Ident(n) =>
          val res = names(n)
          //          println(s"For name=$n idx=$res")
          List(res)
        case Apply(TypeApply(Select(tup, _), _), inner) if allowedApplies.exists(tup.tpe <:< _) =>
//          println(s"Tuple case cls=${tup.getClass} $tup")
          inner.flatMap(toGroupings(_, names))
      }

    def handleBlock(args: List[Statement], body: Tree): List[List[Int]] = {
      val argNames = args.map(_.asInstanceOf[Definition].name).zipWithIndex.toMap
//      println(s"Args: $argNames")
//      println(body.getClass)
//      println(body.show)
      body match {
        // TODO: handle single arg?
        case Apply(TypeApply(Select(tup, _), _), inner) if allowedApplies.exists(tup.tpe <:< _) =>
//          println(s"Tuple cls=${tup.getClass} $tup")
          inner.map(toGroupings(_, argNames))
      }
    }

    val groupingIndices = groupingSet match {
      case Block(List(DefDef(_, List(p), _, Some(body))), _) if p.params.forall(_.isInstanceOf[Definition]) =>
//        println(s"Block body=$body")
        handleBlock(p.params, body)
    }

    val sets      = groupingIndices
    val orderings = metas.map(_.ordering)

    val metasWithIndex = metas.zipWithIndex

    val groupFills = metasWithIndex.map { case (_, idx) =>
      '{ ${ Expr(idx) } -> ((a: Any) => Some(a)) }
    }

    val defaultFills = metasWithIndex.map { case (_, idx) => '{ ${ Expr(idx) } -> None } }

    '{
      Query.GroupingSetsDescription(
        values = ${ Expr(sets) },
        orderings = ${ Expr.ofList(orderings) },
        groupFillments = ${ Expr.ofList(groupFills) }.toMap,
        defaultFillments = ${ Expr.ofList(defaultFills) }.toMap
      )
    }
  }
}
