package scalaql.interpreter

import scalaql.internal.CollectorInterpreter
import scalaql.internal.FindInterpreter
import scalaql.internal.ForeachInterpreter
import scalaql.internal.FunctionK
import scalaql.Query
import scalaql.ToFrom
import scalaql.unstableApi
import scala.collection.mutable.ListBuffer

@unstableApi
trait QueryInterpreter[Param[_]] {
  type Res[In, Out]

  def interpret[In: ToFrom, Out](in: In, query: Query[In, Out])(param: Param[Out]): Res[In, Out]
}

@unstableApi
object QueryInterpreter {

  type Aux[Param[_], Res0[_, _]] = QueryInterpreter[Param] { type Res[In, Out] = Res0[In, Out] }
  type Nullary[Res0[_, _]]       = Aux[λ[a => Unit], Res0]

  val runFind: QueryInterpreter.Aux[* => Boolean, λ[(in, out) => Option[out]]] = FindInterpreter
  val runForeach: QueryInterpreter.Aux[* => Unit, λ[(in, out) => Unit]]        = ForeachInterpreter

  def runCollectBuffer: QueryInterpreter.Nullary[λ[(in, out) => ListBuffer[out]]] =
    runCollect[ListBuffer](FunctionK.identity[ListBuffer])

  def runCollectList: QueryInterpreter.Nullary[λ[(in, out) => List[out]]] =
    runCollect[List](FunctionK.listBufferToList)

  def runCollect[Coll[_]](mapResult: FunctionK[ListBuffer, Coll]): QueryInterpreter.Nullary[λ[(in, out) => Coll[out]]] =
    new CollectorInterpreter[Coll](mapResult)
}
