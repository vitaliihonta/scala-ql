package scalaql.utils

import scala.annotation.tailrec
import scala.quoted.*

object Scala3MacroUtils {
  private case class CallChain(chain: List[String])

  def accessorName[A, B](f: Expr[A => B])(using Quotes): String = {
    import quotes.reflect.*

    def validateEachCall(call: Term): Unit = {
      val TypeApply(Ident(method), rest) = call
      if (method.toString != "each")
        throw new IllegalArgumentException(
          s"Only `.each` and case class accessor call is allowed inside builder"
        )
    }

    @tailrec
    def buildCallChain(tree: Tree, acc: CallChain): CallChain =
      tree match {
        case Ident(_)            => acc
        case Select(inner, name) => buildCallChain(inner, acc.copy(chain = name :: acc.chain))
        case Apply(callable, List(inner)) =>
          validateEachCall(callable)
          buildCallChain(inner, acc)
        case _ =>
          throw new IllegalArgumentException(
            s"Expected a field selector to be passed (as instance.field1), got $tree"
          )
      }

    Expr.betaReduce(f).asTerm.underlying match {
      case Block(List(DefDef(_, _, _, Some(tree))), _) =>
        val chain = buildCallChain(tree, CallChain(Nil))
        getLastField(chain).getOrElse(
          throw new IllegalArgumentException(s"Expected a field selector to be passed (as instance.field1), got $f")
        )
      case _ =>
        throw new IllegalArgumentException(s"Expected a field selector to be passed (as instance.field1), got $f")
    }
  }

  private def getLastField(chain: CallChain): Option[String] =
    for {
      // TODO: add path validation
      last <- chain.chain.lastOption
    } yield last
}
