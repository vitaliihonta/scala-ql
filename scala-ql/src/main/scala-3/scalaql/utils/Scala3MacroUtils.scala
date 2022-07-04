package scalaql.utils

import scala.annotation.tailrec
import scala.quoted.*

object Scala3MacroUtils {

  case class Call(name: String, method: Boolean)
  case class CallChain(chain: List[Call])

  val noop: Any => Unit = value => println(value)
}

class Scala3MacroUtils[Q <: Quotes](using val q: Q) {
  import q.reflect.*
  import Scala3MacroUtils.*

  def validateEachCall(call: Term): Unit = {
    val TypeApply(Ident(method), rest) = call
    if (method.toString != "each")
      throw new IllegalArgumentException(
        s"Only `.each` and case class accessor call is allowed inside builder"
      )
  }

  def accessorName[A, B](f: Expr[A => B]): String = {
    import quotes.reflect.*

    Expr.betaReduce(f).asTerm.underlying match {
      case Block(List(DefDef(_, _, _, Some(tree))), _) =>
        val onNonField: CallChain => CallChain = _ =>
          throw new IllegalArgumentException(
            s"Expected a field selector to be passed (as instance.field1), got $tree"
          )
        val chain = getCallChain(tree, validateEachCall, onNonField)
        chain.chain.lastOption
          .map(_.name)
          .getOrElse(
            throw new IllegalArgumentException(s"Expected a field selector to be passed (as instance.field1), got $f")
          )
      case _ =>
        throw new IllegalArgumentException(s"Expected a field selector to be passed (as instance.field1), got $f")
    }
  }

  //TODO: not working
  def getCallChain[A, B](f: Expr[A => B], validateCall: Term => Unit, onNonField: CallChain => CallChain): CallChain =
    buildCallChain(Expr.betaReduce(f).asTerm.underlying, CallChain(Nil), validateCall, onNonField, inMethod = false)

  def getCallChain(tree: Tree, validateCall: Term => Unit, onNonField: CallChain => CallChain): CallChain =
    buildCallChain(tree, CallChain(Nil), validateCall, onNonField, inMethod = false)

  @tailrec
  private def buildCallChain(
    tree:         Tree,
    acc:          CallChain,
    validateCall: Term => Unit,
    onNonField:   CallChain => CallChain,
    inMethod:     Boolean
  ): CallChain =
    tree match {
      case Ident(_) => acc
      case Select(inner, name) =>
        buildCallChain(
          inner,
          acc.copy(chain = Call(name, inMethod) :: acc.chain),
          validateCall,
          onNonField,
          inMethod = false
        )
      case Apply(callable, List(inner)) =>
        validateCall(callable)
        buildCallChain(inner, acc, validateCall, onNonField, inMethod = true)
      case _ =>
        // TODO: make this reusable for orderBy
        onNonField(acc)
    }
}
