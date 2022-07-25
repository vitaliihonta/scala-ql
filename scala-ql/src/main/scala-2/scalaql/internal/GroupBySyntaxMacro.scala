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
    val query         = getPrefixOf[GroupBySyntax[In, Out]]
    val GroupedQuery1 = weakTypeOf[GroupedQuery1[In, Out, A]].dealias
    val (group, kind) = getGroupAndKind[Out, A](f)

    c.Expr[GroupedQuery1[In, Out, A]](
      q"""
         new $GroupedQuery1($query.self, $group, $kind.widenIn)
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
