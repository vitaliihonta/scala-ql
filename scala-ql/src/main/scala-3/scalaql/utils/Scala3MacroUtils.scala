package scalaql.utils

import scala.annotation.tailrec
import scala.quoted.*

object Scala3MacroUtils {

  case class Call(name: String, method: Boolean)

  def accessorCallPath[A, B](
    using q:     Quotes
  )(f:           Expr[A => B],
    onUncmached: (q.reflect.Term, List[Call]) => List[Call]
  ): List[Call] = {
    import q.reflect.*

    Expr.betaReduce(f).asTerm.underlying match {
      case Block(List(DefDef(_, _, _, Some(term))), _) =>
        selectorPath(term, onUncmached)
      case term =>
        onUncmached(term, Nil)
    }
  }
  // Recursively extracts names from call chain
  def selectorPath(
    using q:     Quotes
  )(term:        q.reflect.Term,
    onUncmached: (q.reflect.Term, List[Call]) => List[Call]
  ): List[Call] = {
    import q.reflect.*

    @tailrec
    def go(term: Term, acc: List[Call]): List[Call] =
      term match {
        case Ident(_)                                        => acc
        case Select(This(_), name)                           => Call(name, method = false) :: acc
        case Select(rest, name)                              => go(rest, Call(name.toString, method = false) :: acc)
        case Apply(TypeApply(Ident(method), _), List(inner)) => go(inner, Call(method.toString, method = true) :: acc)
        case Inlined(_, _, rest)                             => go(rest, acc)
        case _                                               => onUncmached(term, acc)
      }

    go(term, acc = Nil)
  }

  def ignoreUnmatched(using q: Quotes)(term: q.reflect.Term, acc: List[Call]): List[Call] =
    acc

  def throwOnlySelectorIsAllowed(using q: Quotes)(term: q.reflect.Term, acc: List[Call]): List[Call] =
    throw new IllegalArgumentException(s"Expected a field selector to be passed (as instance.field1), got $term")

  def ensureOnlyEachMethod(chain: List[Call]): Unit =
    chain.foreach { call =>
      if (call.method && call.name != "each") {
        throw new IllegalArgumentException(
          s"Only `.each` and case class accessor call is allowed inside builder"
        )
      }
    }

  def getOrdering[A: Type](
    callChain: List[Call],
    ordering:  Expr[Ordering[A]]
  )(using Quotes
  ): Expr[Ordering[A]] =
    callChain.lastOption match {
      case Some(Call("desc", _)) =>
        '{ $ordering.reverse }
      case other =>
        ordering
    }
}
