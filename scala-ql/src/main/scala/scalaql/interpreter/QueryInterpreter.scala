package scalaql.interpreter

import scalaql.internal.CollectorInterpreter
import scalaql.internal.FindInterpreter
import scalaql.internal.FunctionK
import scalaql.Query
import scalaql.ToFrom
import scalaql.unstableApi
import scala.collection.mutable.ListBuffer

@unstableApi
trait QueryInterpreter[Param[_]] {
  type Res[Out]

  def interpret[In: ToFrom, Out](in: In, query: Query[In, Out])(param: Param[Out]): Res[Out]
}

@unstableApi
object QueryInterpreter {

  type Aux[Param[_], Res0[_]] = QueryInterpreter[Param] { type Res[Out] = Res0[Out] }
  type Nullary[Res0[_]]       = Aux[λ[a => Unit], Res0]

  val runFind: QueryInterpreter.Aux[* => Boolean, Option] = FindInterpreter

  def runCollectBuffer: QueryInterpreter.Nullary[ListBuffer] =
    runCollect[ListBuffer](FunctionK.identity[ListBuffer])

  def runCollectList: QueryInterpreter.Nullary[List] =
    runCollect[List](FunctionK.listBufferToList)

  def runCollect[Coll[_]](mapResult: FunctionK[ListBuffer, Coll]): QueryInterpreter.Nullary[Coll] =
    new CollectorInterpreter[Coll](mapResult)
}
