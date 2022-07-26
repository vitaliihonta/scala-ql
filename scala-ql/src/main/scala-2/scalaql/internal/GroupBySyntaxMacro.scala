package scalaql.internal

import scalaql.*
import scalaql.syntax.GroupBySyntax
import scala.reflect.macros.blackbox

class GroupBySyntaxMacro(override val c: blackbox.Context) extends MacroUtils(c)("groupBy") {
  import c.universe.*

  private val Rollup      = TermName("rollup")
  private val Any         = typeOf[Any]
  private val AnyOrdering = typeOf[Ordering[Any]]
  private val OrderingTC  = AnyOrdering.typeConstructor

  def groupBy1Impl[In: WeakTypeTag, Out: WeakTypeTag, A: WeakTypeTag](
    f: Expr[Out => A]
  ): Expr[GroupedQuery1[In, Out, A]] = {
    // TODO: extract self at compile time
    val query         = getPrefixOf[GroupBySyntax[In, Out]]
    val GroupedQuery1 = weakTypeOf[GroupedQuery1[In, Out, A]].dealias
    val (group, kind) = getGroupAndKind[Out, A](f)

    val InTag  = summonTag[In]
    val OutTag = summonTag[Out]
    val ATag   = summonTag[A]

    c.Expr[GroupedQuery1[In, Out, A]](
      q"""
         new $GroupedQuery1($query.self, $group, $kind.widenIn)(
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

    val (group1, kind1) = getGroupAndKind[Out, A](f1)
    val (group2, kind2) = getGroupAndKind[Out, B](f2)

    val InTag  = summonTag[In]
    val OutTag = summonTag[Out]
    val ATag   = summonTag[A]
    val BTag   = summonTag[B]

    c.Expr[GroupedQuery2[In, Out, A, B]](
      q"""
         new $GroupedQuery2($query.self, $group1, $group2, $kind1.widenIn, $kind2.widenIn)(
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

    val (group1, kind1) = getGroupAndKind[Out, A](f1)
    val (group2, kind2) = getGroupAndKind[Out, B](f2)
    val (group3, kind3) = getGroupAndKind[Out, C](f3)

    val InTag  = summonTag[In]
    val OutTag = summonTag[Out]
    val ATag   = summonTag[A]
    val BTag   = summonTag[B]
    val CTag   = summonTag[C]

    c.Expr[GroupedQuery3[In, Out, A, B, C]](
      q"""
         new $GroupedQuery3($query.self, $group1, $group2, $group3, $kind1.widenIn, $kind2.widenIn, $kind3.widenIn)(
           $InTag, $OutTag, $ATag, $BTag, $CTag
         )
       """.debugged("Generated groupBy")
    )
  }

  private def getGroupAndKind[Out: WeakTypeTag, A: WeakTypeTag](f: Expr[Out => A]): (Tree, Tree) = {
    val Out       = weakTypeOf[Out].dealias
    val A         = weakTypeOf[A].dealias
    val callChain = getCallChain(f.tree)
    println(s"$f")
    println(callChain)
    callChain.chain.lastOption match {
      case Some(Rollup) =>
        val theIn = A.dealias.typeArgs.head
        val out   = freshTermName("out")
        val a     = freshTermName("a")
        println("Summon implicit")
        val t = appliedType(OrderingTC, theIn)
        println(t)
        val ordering = summonImplicitT(t)
        println(s"Summoned $ordering")
        val group = q"(($out: $Out) => $f($out).get)".debugged("group")
        val kind = q"""_root_.scalaql.Query.GroupKind.Rollup[$Any, $A](
             ($a: $Any) => _root_.scala.Some($a.asInstanceOf[$theIn]),
             _root_.scala.None, 
             $ordering.asInstanceOf[$AnyOrdering]
           )""".debugged("kind")
        group -> kind

      case _ =>
        val orderingOptTree = summonOptionalImplicit[Ordering[A]]
        val orderingOpt =
          orderingOptTree.fold[Tree](ifEmpty = q"_root_.scala.None")(ord => q"""_root_.scala.Some($ord)""")
        f.tree -> q"_root_.scalaql.Query.GroupKind.Simple[$A]($orderingOpt)"
    }
  }
}
