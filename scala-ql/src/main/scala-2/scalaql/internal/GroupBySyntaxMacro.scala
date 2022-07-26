package scalaql.internal

import scalaql.*
import scalaql.syntax.GroupBySyntax
import scala.reflect.macros.blackbox

class GroupBySyntaxMacro(override val c: blackbox.Context) extends MacroUtils(c)("groupBy") {
  import c.universe.*

  private val Rollup       = TermName("rollup")
  private val Any          = typeOf[Any]
  private val AnyOrdering  = typeOf[Ordering[Any]]
  private val GroupingSet  = typeOf[Query.GroupingSet].dealias
  private val GroupingSets = typeOf[Query.GroupingSets].dealias
  private val OrderingTC   = AnyOrdering.typeConstructor

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

  private implicit val LiftableGroupingSet: Liftable[Query.GroupingSet] = Liftable[Query.GroupingSet] { set =>
    val res = Apply(q"_root_.scalaql.Query.GroupingSet", List(implicitly[Liftable[List[Int]]].apply(set.value)))
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
      case Some(Rollup) =>
        val theIn = A.dealias.typeArgs.head
        val out   = freshTermName("out")
        val a     = freshTermName("a")
//        println("Summon implicit")
        val t = appliedType(OrderingTC, theIn)
//        println(t)
        val ordering = summonImplicitT(t)
//        println(s"Summoned $ordering")
        val group            = q"(($out: $Out) => $f($out).get)".debugged("group")
        val kind             = KindRollup
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
        List(Query.GroupingSet(metasWithIndex.map { case (_, idx) => idx }))
      } else {
        nonSimple.head match {
          // TODO: implement this shit
          case KindCube => ???
          case KindRollup if allNonSimple =>
            metasWithIndex.tails.map(tail => Query.GroupingSet(tail.map { case (_, idx) => idx })).toList
          case KindRollup =>
            val (partialKeys, subtotalKeys) = metasWithIndex.partition { case (m, _) => m.kind == KindSimple }

            val subtotals = (1 until subtotalKeys.size).flatMap { n =>
              subtotalKeys
                .map { case (_, idx) => idx }
                .combinations(n)
                .filterNot(_.isEmpty)
                .map(sub => Query.GroupingSet((sub.toList ++ partialKeys.map { case (_, idx) => idx }).distinct))
            }.toList

            val partial = Query.GroupingSet(partialKeys.map { case (_, idx) => idx })
            val all     = Query.GroupingSet(metasWithIndex.map { case (_, idx) => idx })

            (all :: partial :: subtotals).distinct.reverse
        }
      }
    }

    val orderings    = metas.map(_.ordering)
    val groupFills   = metasWithIndex.map { case (m, idx) => q"""$idx -> ${m.groupFillments}""" }
    val defaultFills = metasWithIndex.flatMap { case (m, idx) => m.defaultFillments.map(df => q"""$idx -> $df""") }

    q"""
      _root_.scalaql.Query.GroupingSets(
        values = List(..$sets),
        orderings = List(..$orderings),
        groupFillments = Map(..$groupFills),
        defaultFillments = Map(..$defaultFills)
      )
      """
  }
}
