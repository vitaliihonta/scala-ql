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
trait QueryInterpreter[Param[_]] extends Serializable {
  type Res[Out]

  def interpret[In: ToFrom, Out](in: In, query: Query[In, Out])(param: Param[Out]): Res[Out]
}

@unstableApi
object QueryInterpreter {

  final type Aux[Param[_], Res0[_]] = QueryInterpreter[Param] { type Res[Out] = Res0[Out] }
  final type Nullary[Res0[_]]       = Aux[λ[a => Unit], Res0]

  val runFind: QueryInterpreter.Aux[* => Boolean, Option]       = FindInterpreter
  val runForeach: QueryInterpreter.Aux[* => Unit, λ[a => Unit]] = ForeachInterpreter

  def runCollectBuffer: QueryInterpreter.Nullary[ListBuffer] =
    runCollect[ListBuffer](FunctionK.identity[ListBuffer])

  def runCollectList: QueryInterpreter.Nullary[List] =
    runCollect[List](FunctionK.listBufferToList)

  def runCollect[Coll[_]](mapResult: FunctionK[ListBuffer, Coll]): QueryInterpreter.Nullary[Coll] =
    new CollectorInterpreter[Coll](mapResult)
}
