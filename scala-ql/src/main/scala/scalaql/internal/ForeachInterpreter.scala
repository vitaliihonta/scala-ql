package scalaql.internal

import scalaql.ToFrom
import scalaql.Query
import scalaql.interpreter.QueryInterpreter

private[scalaql] object ForeachInterpreter extends QueryInterpreter[* => Unit] {
  override type Res[Out] = Unit

  override def interpret[In: ToFrom, Out](in: In, query: Query[In, Out])(f: Out => Unit): Unit =
    InternalQueryInterpreter.interpret[In, Out](in, query)(
      Step.always[Out](f)
    )
}
