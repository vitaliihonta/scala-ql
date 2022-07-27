package scalaql.internal

import scalaql.*
import scalaql.syntax.GroupBySyntax
import scala.reflect.macros.blackbox

class GroupBySyntaxMacro(override val c: blackbox.Context) extends MacroUtils(c)("groupBy") {
  import c.universe.*

  private val Rollup      = TermName("rollup")
  private val Cube        = TermName("cube")
  private val Any         = typeOf[Any]
  private val AnyOrdering = typeOf[Ordering[Any]]
  private val OrderingTC  = AnyOrdering.typeConstructor

  private sealed trait GroupingKind
  private case object KindSimple extends GroupingKind
  private case object KindRollup extends GroupingKind
  private case object KindCube   extends GroupingKind
  private case class GroupingMeta(
    groupFuncBody:    Tree,
    kind:             GroupingKind,
    ordering:         Tree,
    groupFillments:   Tree,
    defaultFillments: Option[Tree])

  private implicit val LiftableGroupingSetIndices: Liftable[Query.GroupingSetIndices] =
    Liftable[Query.GroupingSetIndices] { set =>
      val res =
        Apply(q"_root_.scalaql.Query.GroupingSetIndices", List(implicitly[Liftable[List[Int]]].apply(set.value)))

      println(s"Lifted $res")
      res
    }

  def groupBy1Impl[In: WeakTypeTag, Out: WeakTypeTag, A: WeakTypeTag](
    f: Expr[Out => A]
  ): Expr[GroupedQuery1[In, Out, A]] = {
    // TODO: extract self at compile time
    val query         = getPrefixOf[GroupBySyntax[In, Out]]
    val GroupedQuery1 = weakTypeOf[GroupedQuery1[In, Out, A]].dealias
    val meta          = getGroupingMeta[Out, A](f)
    val groupingSets  = buildGroupingSets(List(meta))

    val InTag  = summonTag[In]
    val OutTag = summonTag[Out]
    val ATag   = summonTag[A]

    c.Expr[GroupedQuery1[In, Out, A]](
      q"""
         new $GroupedQuery1($query.self, ${meta.groupFuncBody}, $groupingSets)(
           $InTag, $OutTag, $ATag
         )
       """.debugged("Generated groupBy")
    )
  }

  def groupBy2Impl[In: WeakTypeTag, Out: WeakTypeTag, A: WeakTypeTag, B: WeakTypeTag](
    f1: Expr[Out => A],
    f2: Expr[Out => B]
  ): Expr[GroupedQuery2[In, Out, A, B]] = {
    // TODO: extract self at compile time
    val query         = getPrefixOf[GroupBySyntax[In, Out]]
    val GroupedQuery2 = weakTypeOf[GroupedQuery2[In, Out, A, B]].dealias

    val meta1        = getGroupingMeta[Out, A](f1)
    val meta2        = getGroupingMeta[Out, B](f2)
    val groupingSets = buildGroupingSets(List(meta1, meta2))

    val InTag  = summonTag[In]
    val OutTag = summonTag[Out]
    val ATag   = summonTag[A]
    val BTag   = summonTag[B]

    c.Expr[GroupedQuery2[In, Out, A, B]](
      q"""
         new $GroupedQuery2($query.self, ${meta1.groupFuncBody}, ${meta2.groupFuncBody}, $groupingSets)(
           $InTag, $OutTag, $ATag, $BTag
         )
       """.debugged("Generated groupBy")
    )
  }

  // TODO: implement for 3 keys
  def groupBy2GroupingSetsImpl[In: WeakTypeTag, Out: WeakTypeTag, A: WeakTypeTag, B: WeakTypeTag](
    f1:           Expr[Out => A],
    f2:           Expr[Out => B]
  )(groupingSets: Expr[(A, B) => Product]
  ): Expr[GroupedQuery2[In, Out, Option[A], Option[B]]] = {
    // TODO: extract self at compile time
    val query         = getPrefixOf[GroupBySyntax[In, Out]]
    val GroupedQuery2 = weakTypeOf[GroupedQuery2[In, Out, Option[A], Option[B]]].dealias

    val meta1            = getGroupingMeta[Out, A](f1)
    val meta2            = getGroupingMeta[Out, B](f2)
    val groupingSetsTree = extractGroupingSets(groupingSets.tree, List(meta1, meta2))

    val InTag  = summonTag[In]
    val OutTag = summonTag[Out]
    val ATag   = summonTag[Option[A]]
    val BTag   = summonTag[Option[B]]

    c.Expr[GroupedQuery2[In, Out, Option[A], Option[B]]](
      q"""
         new $GroupedQuery2($query.self, ${meta1.groupFuncBody}, ${meta2.groupFuncBody}, $groupingSetsTree)(
           $InTag, $OutTag, $ATag, $BTag
         )
       """.debugged("Generated groupBy")
    )
  }

  def groupBy3Impl[In: WeakTypeTag, Out: WeakTypeTag, A: WeakTypeTag, B: WeakTypeTag, C: WeakTypeTag](
    f1: Expr[Out => A],
    f2: Expr[Out => B],
    f3: Expr[Out => C]
  ): Expr[GroupedQuery3[In, Out, A, B, C]] = {
    // TODO: extract self at compile time
    val query         = getPrefixOf[GroupBySyntax[In, Out]]
    val GroupedQuery3 = weakTypeOf[GroupedQuery3[In, Out, A, B, C]].dealias

    val meta1        = getGroupingMeta[Out, A](f1)
    val meta2        = getGroupingMeta[Out, B](f2)
    val meta3        = getGroupingMeta[Out, C](f3)
    val groupingSets = buildGroupingSets(List(meta1, meta2, meta3))

    val InTag  = summonTag[In]
    val OutTag = summonTag[Out]
    val ATag   = summonTag[A]
    val BTag   = summonTag[B]
    val CTag   = summonTag[C]

    c.Expr[GroupedQuery3[In, Out, A, B, C]](
      q"""
         new $GroupedQuery3($query.self, ${meta1.groupFuncBody}, ${meta2.groupFuncBody}, ${meta3.groupFuncBody}, $groupingSets)(
           $InTag, $OutTag, $ATag, $BTag, $CTag
         )
       """.debugged("Generated groupBy")
    )
  }

  private def getGroupingMeta[Out: WeakTypeTag, A: WeakTypeTag](f: Expr[Out => A]): GroupingMeta = {
    val Out       = weakTypeOf[Out].dealias
    val A         = weakTypeOf[A].dealias
    val callChain = getCallChain(f.tree)
//    println(s"$f")
//    println(callChain)
    callChain.chain.lastOption match {
      // TODO: handle fillna
      case Some(term @ (Rollup | Cube)) =>
        val theIn = A.dealias.typeArgs.head
        val out   = freshTermName("out")
        val a     = freshTermName("a")
//        println("Summon implicit")
        val t = appliedType(OrderingTC, theIn)
//        println(t)
        val ordering = summonImplicitT(t)
//        println(s"Summoned $ordering")
        val group            = q"(($out: $Out) => $f($out).get)".debugged("group")
        val kind             = if (term == Cube) KindCube else KindRollup
        val groupFillment    = q"""($a: $Any) => _root_.scala.Some($a.asInstanceOf[$theIn])"""
        val defaultFillments = Some(q"""_root_.scala.None""")
        GroupingMeta(
          groupFuncBody = group,
          kind = kind,
          ordering = q"$ordering.asInstanceOf[$AnyOrdering]",
          groupFillments = groupFillment,
          defaultFillments = defaultFillments
        )

      case _ =>
        val group = f.tree
        val kind  = KindSimple
        val ordering = summonOptionalImplicit[Ordering[A]]
          .getOrElse(reify(NaturalOrdering[A]).tree)
        val groupFillment = {
          val a = freshTermName("a")
          q"""($a: $Any) => $a"""
        }
        val defaultFillments = None
        GroupingMeta(
          groupFuncBody = group,
          kind = kind,
          ordering = q"$ordering.asInstanceOf[$AnyOrdering]",
          groupFillments = groupFillment,
          defaultFillments = defaultFillments
        )
    }
  }

  private val allowedApplies = Set(
    typeOf[Tuple1.type].dealias,
    typeOf[Tuple2.type].dealias,
    typeOf[Tuple3.type].dealias,
    typeOf[Tuple4.type].dealias,
    typeOf[Tuple5.type].dealias,
    typeOf[Tuple6.type].dealias,
    typeOf[Tuple7.type].dealias,
    typeOf[Tuple8.type].dealias
  )

  private def extractGroupingSets(groupingSet: Tree, metas: List[GroupingMeta]): Tree = {
    def toGroupings(tree: Tree, names: Map[Name, Int]): List[Int] = {
      println(tree)
      println(tree.getClass)
      tree match {
        case Literal(Constant(())) =>
          Nil
        case Ident(n) =>
          val res = names(n)
          println(s"For name=$n idx=$res")
          List(res)
        case Apply(TypeApply(Select(tup, _), _), inner) if allowedApplies.exists(tup.tpe <:< _) =>
          println(s"Tuple case $inner")
          inner.flatMap(toGroupings(_, names))
      }
    }
    println(groupingSet)
    println(groupingSet.getClass)
    val groupingIndices = groupingSet match {
      case Function(args, body) =>
        val argNames = args.map(_.name: Name).zipWithIndex.toMap
        println(s"Args: $argNames")
//        println(body.getClass)
        body match {
          // TODO: handle single arg?
          case Apply(TypeApply(Select(tup, _), _), inner) if allowedApplies.exists(tup.tpe <:< _) =>
            inner.map(toGroupings(_, argNames))
        }
    }

    val sets      = groupingIndices.map(Query.GroupingSetIndices)
    val orderings = metas.map(_.ordering)

    val metasWithIndex = metas.zipWithIndex
    val groupFills = metasWithIndex.map { case (_, idx) =>
      val a = freshTermName("a")
      q"""$idx -> (($a: $Any) => _root_.scala.Some($a))"""
    }
    val defaultFills = metasWithIndex.map { case (_, idx) => q"""$idx -> _root_.scala.None""" }

    q"""
      _root_.scalaql.Query.GroupingSetsDescription(
        values = List(..$sets),
        orderings = List(..$orderings),
        groupFillments = Map(..$groupFills),
        defaultFillments = Map(..$defaultFills)
      )
      """
  }

  private def buildGroupingSets(metas: List[GroupingMeta]): Tree = {
    val kinds     = metas.map(_.kind).toSet
    val nonSimple = kinds - KindSimple
    if (nonSimple.size > 1) {
      error(s"It is not allowed to mix ${nonSimple.mkString(" and ")} in a single groupBy")
    }
    val metasWithIndex = metas.zipWithIndex
    println(s"metasWithIndex=$metasWithIndex")
    val sets = {
      val isSimple     = nonSimple.isEmpty
      def allNonSimple = metas.forall(_.kind != KindSimple)
      if (isSimple) {
        List(Query.GroupingSetIndices(metasWithIndex.map { case (_, idx) => idx }))
      } else {
        nonSimple.head match {
          case KindSimple =>
            error(FatalExceptions.libraryErrorMessage(s"Non-simple grouping kinds contains KindSimple metas=$metas"))
          // TODO: implement this shit
          case KindCube => ???
          case KindRollup if allNonSimple =>
            metasWithIndex.tails.map(tail => Query.GroupingSetIndices(tail.map { case (_, idx) => idx })).toList
          case KindRollup =>
            val (partialKeys, subtotalKeys) = metasWithIndex.partition { case (m, _) => m.kind == KindSimple }

            val subtotals = (1 until subtotalKeys.size).flatMap { n =>
              subtotalKeys
                .map { case (_, idx) => idx }
                .combinations(n)
                .filterNot(_.isEmpty)
                .map(sub => Query.GroupingSetIndices((sub.toList ++ partialKeys.map { case (_, idx) => idx }).distinct))
            }.toList

            val partial = Query.GroupingSetIndices(partialKeys.map { case (_, idx) => idx })
            val all     = Query.GroupingSetIndices(metasWithIndex.map { case (_, idx) => idx })

            (all :: partial :: subtotals).distinct.reverse
        }
      }
    }

    val orderings    = metas.map(_.ordering)
    val groupFills   = metasWithIndex.map { case (m, idx) => q"""$idx -> ${m.groupFillments}""" }
    val defaultFills = metasWithIndex.flatMap { case (m, idx) => m.defaultFillments.map(df => q"""$idx -> $df""") }

    q"""
      _root_.scalaql.Query.GroupingSetsDescription(
        values = List(..$sets),
        orderings = List(..$orderings),
        groupFillments = Map(..$groupFills),
        defaultFillments = Map(..$defaultFills)
      )
      """
  }
}
