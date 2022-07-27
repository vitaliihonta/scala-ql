package scalaql.internal

import scalaql.Tag
import scalaql.syntax.{EachSyntaxIterable, EachSyntaxOption, GroupingSetsOps}

import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

abstract class MacroUtils(val c: blackbox.Context)(prefix: String) {
  import c.universe.*

  private val EachSyntaxIterable = weakTypeOf[EachSyntaxIterable[Any]].dealias.typeConstructor
  private val EachSyntaxOption   = weakTypeOf[EachSyntaxOption[Any]].dealias.typeConstructor
  private val GroupingSetsOps    = weakTypeOf[GroupingSetsOps[Any]].dealias.typeConstructor
  private val Desc               = TermName("desc")

  protected def freshTermName(name: String): TermName = c.freshName(TermName(name))

  protected def extractSelectorField(t: Tree): Option[TermName] = {
    val chain = getCallChain(t)
    getLastField(chain)
  }

  protected def getCallChain(t: Tree): CallChain =
    buildCallChain(t, CallChain(None, Nil, isFieldAccessor = false))

  protected def summonImplicitT(t: Type): Tree =
    c.inferImplicitValue(t, silent = false)

  protected def summonImplicit[A: WeakTypeTag]: Tree =
    summonImplicitT(weakTypeOf[A])

  protected def summonOptionalImplicit[A: WeakTypeTag]: Option[Tree] =
    summonOptionalImplicitT(weakTypeOf[A])

  protected def summonOptionalImplicitT(t: Type): Option[Tree] =
    try
      Some(summonImplicitT(t))
    catch {
      case _: c.TypecheckException => None
    }

  protected def getOrdering[B](callChain: CallChain, ordering: Expr[Ordering[B]]): Expr[Ordering[B]] =
    callChain.chain.lastOption match {
      case Some(Desc) =>
        c.Expr[Ordering[B]](q"$ordering.reverse")
      case _ => ordering
    }

  protected def summonTag[A: WeakTypeTag]: Tree =
    summonImplicit[Tag[A]]

  protected def getPrefixOf[Prefix: WeakTypeTag]: Expr[Prefix] = {
    val Prefix = weakTypeOf[Prefix]
    if (!(c.prefix.tree.tpe =:= Prefix)) {
      error(s"Invalid library usage! Expected to be macro expanded within $Prefix, instead it's ${c.prefix.tree.tpe}")
    }
    c.Expr[Prefix](
      c.prefix.tree
    )
  }

  protected case class CallChain(obj: Option[ValDef], chain: List[TermName], isFieldAccessor: Boolean)

  @tailrec
  private def buildCallChain(tree: Tree, acc: CallChain): CallChain =
    if (tree.isEmpty) acc
    else
      tree match {
        case q"(${vd: ValDef}) => $inner" =>
          buildCallChain(inner, acc.copy(obj = Some(vd)))
        case q"${idt: Ident}.${fieldName: TermName}" =>
          acc.copy(chain = fieldName :: acc.chain, isFieldAccessor = acc.obj.exists(_.name == idt.name))
        case Select(qualifier, name) =>
          buildCallChain(qualifier, acc.copy(chain = name.toTermName :: acc.chain))
        case Apply(callable, List(inner)) =>
          val tc = callable.tpe.resultType.typeConstructor
          if (tc <:< EachSyntaxIterable || tc <:< EachSyntaxOption || tc <:< GroupingSetsOps) {
            buildCallChain(inner, acc)
          } else {
            acc
          }
        case _ => acc
      }

  private def getLastField(chain: CallChain): Option[TermName] =
    for {
      _ <- chain.obj
      if chain.isFieldAccessor
      last <- chain.chain.lastOption
    } yield last

  protected def error(message: String): Nothing = c.abort(c.enclosingPosition, message)

  protected def debugEnabled: Boolean =
    sys.props
      .get(s"scala-ql-$prefix.debug.macro")
      .flatMap(str => scala.util.Try(str.toBoolean).toOption)
      .getOrElse(false)

  implicit class Debugged[A](self: A) {

    def debugged(msg: String): A = {
      if (debugEnabled) {
        println(s"$msg: $self")
      }
      self
    }
  }
}
