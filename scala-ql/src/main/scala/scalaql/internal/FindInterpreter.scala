package scalaql.internal

import scalaql._
import scalaql.interpreter.QueryInterpreter

private[scalaql] object FindInterpreter extends QueryInterpreter[* => Boolean] {
  override type Res[Out] = Option[Out]

  override def interpret[In: ToFrom, Out](in: In, query: Query[In, Out])(p: Out => Boolean): Option[Out] = {
    var found = Option.empty[Out]
    InternalQueryInterpreter.interpret[In, Out](in, query)(
      Step[Out](
        check = () => found.isEmpty,
        next = { out => found = Some(out).filter(p) }
      )
    )
    found
  }
}
