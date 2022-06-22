package scalaql.utils

import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

abstract class MacroUtils(val c: blackbox.Context)(prefix: String) {
  import c.universe.*

  private val EachSyntaxIterable = weakTypeOf[EachSyntaxIterable[Any]].dealias.typeConstructor
  private val EachSyntaxOption   = weakTypeOf[EachSyntaxOption[Any]].dealias.typeConstructor

  protected def extractSelectorField(t: Tree): Option[TermName] = {
    val chain = buildCallChain(t, CallChain(None, Nil, false))
    getLastField(chain)
  }

  private case class CallChain(obj: Option[ValDef], chain: List[TermName], validFieldCall: Boolean)

  @tailrec
  private def buildCallChain(tree: Tree, acc: CallChain): CallChain =
    if (tree.isEmpty) acc
    else
      tree match {
        case q"(${vd: ValDef}) => $inner" =>
          buildCallChain(inner, acc.copy(obj = Some(vd)))
        case q"${idt: Ident}.${fieldName: TermName}" =>
          acc.copy(chain = fieldName :: acc.chain, validFieldCall = acc.obj.exists(_.name == idt.name))
        case Select(qualifier, name) =>
          buildCallChain(qualifier, acc.copy(chain = name.toTermName :: acc.chain))
        case Apply(callable, List(inner))
            if callable.tpe.resultType.typeConstructor <:< EachSyntaxIterable | callable.tpe.resultType.typeConstructor <:< EachSyntaxOption =>
          buildCallChain(inner, acc)
        case _ => acc
      }

  private def getLastField(chain: CallChain): Option[TermName] =
    for {
      _ <- chain.obj
      if chain.validFieldCall
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
