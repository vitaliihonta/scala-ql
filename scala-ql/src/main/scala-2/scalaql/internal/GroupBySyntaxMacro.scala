package scalaql.internal

import scalaql.*
import scalaql.syntax.{GroupBySyntax, GroupingSetsFillNAOps}
import GroupByMacroSharedUtils.*
import scala.reflect.macros.blackbox

class GroupBySyntaxMacro(override val c: blackbox.Context) extends MacroUtils(c)("groupBy") {
  import c.universe.*

  private val Rollup                = TermName("rollup")
  private val Cube                  = TermName("cube")
  private val Fillna                = TermName("fillna")
  private val GroupingSetsFillNAOps = TermName("GroupingSetsFillNAOps")
  private val ScalaqlPkg            = TermName("scalaql")
  private val Pkg                   = TermName("package")
  private val Any                   = typeOf[Any]
  private val AnyOrdering           = typeOf[Ordering[Any]]
  private val OrderingTC            = AnyOrdering.typeConstructor

  def groupBy1Impl[In: WeakTypeTag, Out: WeakTypeTag, A: WeakTypeTag](
    f: Expr[Out => A]
  ): Expr[GroupedQuery1[In, Out, A]] = {
    // TODO: extract self at compile time
    val query         = getPrefixOf[GroupBySyntax[In, Out]]
    val GroupedQuery1 = weakTypeOf[GroupedQuery1[In, Out, A]].dealias
    val meta          = getGroupingMeta[Out, A](f)
    val groupingSets  = buildGroupingSets(List(meta.widen))

    val InTag  = summonTag[In]
    val OutTag = summonTag[Out]
    val ATag   = summonTag[A]

    c.Expr[GroupedQuery1[In, Out, A]](
      q"""
         new $GroupedQuery1($query.__scalaql_self, ${meta.groupFuncBody}, $groupingSets)(
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
    val groupingSets = buildGroupingSets(List(meta1.widen, meta2.widen))

    val InTag  = summonTag[In]
    val OutTag = summonTag[Out]
    val ATag   = summonTag[A]
    val BTag   = summonTag[B]

    c.Expr[GroupedQuery2[In, Out, A, B]](
      q"""
         new $GroupedQuery2($query.__scalaql_self, ${meta1.groupFuncBody}, ${meta2.groupFuncBody}, $groupingSets)(
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
    val groupingSetsTree = extractGroupingSets(groupingSets.tree, List(meta1.widen, meta2.widen))

    val InTag  = summonTag[In]
    val OutTag = summonTag[Out]
    val ATag   = summonTag[Option[A]]
    val BTag   = summonTag[Option[B]]

    c.Expr[GroupedQuery2[In, Out, Option[A], Option[B]]](
      q"""
         new $GroupedQuery2($query.__scalaql_self, ${meta1.groupFuncBody}, ${meta2.groupFuncBody}, $groupingSetsTree)(
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
    val groupingSets = buildGroupingSets(List(meta1.widen, meta2.widen, meta3.widen))

    val InTag  = summonTag[In]
    val OutTag = summonTag[Out]
    val ATag   = summonTag[A]
    val BTag   = summonTag[B]
    val CTag   = summonTag[C]

    c.Expr[GroupedQuery3[In, Out, A, B, C]](
      q"""
         new $GroupedQuery3($query.__scalaql_self, ${meta1.groupFuncBody}, ${meta2.groupFuncBody}, ${meta3.groupFuncBody}, $groupingSets)(
           $InTag, $OutTag, $ATag, $BTag, $CTag
         )
       """.debugged("Generated groupBy")
    )
  }

  private def getGroupingMeta[Out: WeakTypeTag, A: WeakTypeTag](f: Expr[Out => A]): GroupingMeta[Expr, Out, A] = {
    val Out = weakTypeOf[Out].dealias
    val A   = weakTypeOf[A].dealias

//    println(s"$f")
//    println(callChain)

    val (chainTree, groupFillOverrideOpt, defaultFillOverrideOpt) = f.tree match {
      case Function(
            _,
            Apply(
              Select(
                Apply(
                  TypeApply(Select(Select(Ident(ScalaqlPkg), Pkg), GroupingSetsFillNAOps), _),
                  List(body)
                ),
                Fillna
              ),
              List(inner)
            )
          ) =>
        println(
          s"fillment=$inner body=$body"
        )
        val fill = freshTermName("fill")
        (body, Some(q"($fill: $Any) => $fill"), Some(inner))
      case _ => (f.tree, None, None)
    }

    val callChain = getCallChain(chainTree)
    callChain.chain.lastOption match {
      case Some(term @ (Rollup | Cube)) =>
        println(s"Rollup/Cube A=$A targs=${A.dealias.typeArgs}")
        val theIn = Some(A.dealias)
          .filter(_ <:< typeOf[Option[Any]])
          .flatMap(_.typeArgs.headOption)
          // in case of fillna, A is not option
          .getOrElse(A)
        val out = freshTermName("out")
        val a   = freshTermName("a")
//        println("Summon implicit")
        val t = appliedType(OrderingTC, theIn)
//        println(t)
        val ordering = summonImplicitT(t)
//        println(s"Summoned $ordering")
        // TODO: extract rollup and fillna to avoid wrapping/unwrapping option
        val group = groupFillOverrideOpt
          // in case of fillna = no need to extract the option
          .map[Tree](_ => q"(($out: $Out) => $f($out))")
          .getOrElse[Tree](q"(($out: $Out) => $f($out).get)")

        val kind = if (term == Cube) KindCube else KindRollup

        val groupFillment = groupFillOverrideOpt
          .getOrElse[Tree](q"""($a: $Any) => _root_.scala.Some($a.asInstanceOf[$theIn])""")

        val defaultFillments = defaultFillOverrideOpt
          .orElse[Tree](Some(q"""_root_.scala.None"""))

        GroupingMeta[Expr, Out, A](
          groupFuncBody = c.Expr[Out => A](group),
          kind = kind,
          ordering = c.Expr[Ordering[Any]](q"$ordering.asInstanceOf[$AnyOrdering]"),
          groupFillments = c.Expr[Any => Any](groupFillment),
          defaultFillments = defaultFillments.map(c.Expr[Any](_))
        )
      case _ =>
        val group = f
        val kind  = KindSimple
        val ordering = c.Expr[Ordering[Any]] {
          val base = summonOptionalImplicit[Ordering[A]]
            .getOrElse(reify(NaturalOrdering[A]).tree)
          q"$base.asInstanceOf[$AnyOrdering]"
        }
        val groupFillment = {
          val a = freshTermName("a")
          q"""($a: $Any) => $a"""
        }
        val defaultFillments = None
        GroupingMeta[Expr, Out, A](
          groupFuncBody = group,
          kind = kind,
          ordering = ordering,
          groupFillments = c.Expr[Any => Any](groupFillment),
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

  private def extractGroupingSets(
    groupingSet: Tree,
    metas:       List[GroupingMeta[Expr, Any, Any]]
  ): Expr[Query.GroupingSetsDescription] = {
    def toGroupings(tree: Tree, names: Map[Name, Int]): List[Int] =
//      println(tree)
//      println(tree.getClass)
      tree match {
        case Literal(Constant(())) =>
          Nil
        case Ident(n) =>
          val res = names(n)
//          println(s"For name=$n idx=$res")
          List(res)
        case Apply(TypeApply(Select(tup, _), _), inner) if allowedApplies.exists(tup.tpe <:< _) =>
//          println(s"Tuple case $inner")
          inner.flatMap(toGroupings(_, names))
      }
//    println(groupingSet)
//    println(groupingSet.getClass)
    val groupingIndices = groupingSet match {
      case Function(args, body) =>
        val argNames = args.map(_.name: Name).zipWithIndex.toMap
//        println(s"Args: $argNames")
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

    c.Expr[Query.GroupingSetsDescription](
      q"""
        _root_.scalaql.Query.GroupingSetsDescription(
          values = List(..$sets),
          orderings = List(..$orderings),
          groupFillments = Map(..$groupFills),
          defaultFillments = Map(..$defaultFills)
        )
        """
    )
  }

  private def buildGroupingSets(metas: List[GroupingMeta[Expr, Any, Any]]): Expr[Query.GroupingSetsDescription] =
    GroupByMacroSharedUtils.buildGroupingSets[Expr](metas)(
      error,
      toGroupFills = (m, idx) => c.Expr[(Int, Any => Any)](q"""$idx -> ${m.groupFillments}"""),
      toDefaultFills = (df, idx) => c.Expr[(Int, Any)](q"""$idx -> $df"""),
      buildTree = (sets, orderings, groupFills, defaultFills) => c.Expr[Query.GroupingSetsDescription](q"""
        _root_.scalaql.Query.GroupingSetsDescription(
          values = List(..$sets),
          orderings = List(..$orderings),
          groupFillments = Map(..$groupFills),
          defaultFillments = Map(..$defaultFills)
        )
      """)
    )
}
