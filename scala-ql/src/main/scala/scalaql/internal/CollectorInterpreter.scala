package scalaql.internal

import scalaql._
import scalaql.interpreter.QueryInterpreter
import scala.collection.mutable.ListBuffer

private[scalaql] class CollectorInterpreter[Coll[_]](mapResult: FunctionK[ListBuffer, Coll])
    extends QueryInterpreter[λ[a => Unit]] {

  override type Res[Out] = Coll[Out]

  override def interpret[In: ToFrom, Out](in: In, query: Query[In, Out])(param: Unit): Coll[Out] = {
    val buffer = ListBuffer.empty[Out]
    InternalQueryInterpreter.interpret[In, Out](in, query)(
      Step.always[Out](buffer += _)
    )
    mapResult(buffer)
  }
}
