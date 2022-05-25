package scalaql.internal

import scalaql._
import scalaql.interpreter.QueryInterpreter

@internalApi
private[scalaql] object QueryResultRunner {

  def runImpl[In: ToFrom, Out](queryResult: QueryResult[In, Out])(in: In): Out =
    queryResult match {
      case queryResult: QueryResult.Const[Out] => queryResult.value

      case queryResult: QueryResult.Collect[_, In, Out] =>
        import queryResult._
        QueryInterpreter.runCollect(mapResult).interpret[In, Out](in, query)(()).asInstanceOf[Out]

      case queryResult: QueryResult.CollectMap[In, k, v] =>
        import queryResult._
        QueryInterpreter.runCollectBuffer.interpret[In, (k, v)](in, query)(()).toMap.asInstanceOf[Out]

      case queryResult: QueryResult.Find[In, Out] =>
        import queryResult._
        QueryInterpreter.runFind.interpret[In, Out](in, query)(predicate).asInstanceOf[Out]

      case queryResult: QueryResult.Mapped[In, out0, Out] =>
        import queryResult._
        project(runImpl[In, out0](base)(in))

      case queryResult: QueryResult.FlatMapped[In, out0, ou1] =>
        import queryResult._
        runImpl(
          bind(runImpl[In, out0](base)(in))
        )(in)
    }
}
