package scalaql.internal

import scalaql.ToFrom
import scalaql.internalApi
import scalaql.QueryResult
import scalaql.interpreter.QueryInterpreter

@internalApi
private[scalaql] object QueryResultRunner {

  def runImpl[In: ToFrom, Out](queryResult: QueryResult[In, Out])(in: In): Out =
    queryResult match {
      case queryResult: QueryResult.Const[Out] => queryResult.value

      case queryResult: QueryResult.Collect[?, In, Out] =>
        import queryResult.*
        QueryInterpreter.runCollect(mapResult).interpret[In, Out](in, query)(()).asInstanceOf[Out]

      case queryResult: QueryResult.ForeachWithResource[r, s, In, Out] =>
        import queryResult.*
        val resource = sideEffect.acquire()
        var state    = sideEffect.initialState
        try
          QueryInterpreter.runForeach
            .interpret(in, query) { in =>
              state = sideEffect.use(resource, state, in)
            }
            .asInstanceOf[Out]
        finally {
          sideEffect.release(resource, state)
          state = null.asInstanceOf[s] // free memory
        }

      case queryResult: QueryResult.CollectMap[In, k, v] =>
        import queryResult.*
        QueryInterpreter.runCollectBuffer.interpret[In, (k, v)](in, query)(()).toMap.asInstanceOf[Out]

      case queryResult: QueryResult.Find[In, Out] =>
        import queryResult.*
        QueryInterpreter.runFind.interpret[In, Out](in, query)(predicate).asInstanceOf[Out]

      case queryResult: QueryResult.Mapped[In, out0, Out] =>
        import queryResult.*
        project(runImpl[In, out0](base)(in))

      case queryResult: QueryResult.FlatMapped[In, out0, ou1] =>
        import queryResult.*
        runImpl(
          bind(runImpl[In, out0](base)(in))
        )(in)
    }
}
