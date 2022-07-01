package scalaql.internal

import scalaql.From
import scalaql.ToFrom
import scalaql.AggregationView
import scalaql.Query
import scalaql.interpreter.QueryInterpreter
import scala.collection.mutable.ListBuffer

private[scalaql] object InternalQueryInterpreter extends QueryInterpreter[Step] {

  override type Res[Out] = Unit

  override def interpret[In: ToFrom, Out](in: In, query: Query[In, Out])(step: Step[Out]): Unit = {
    query match {
      case query: Query.Const[Out] =>
        val outputs = query.values.iterator
        while (step.check() && outputs.hasNext)
          step.next(outputs.next())

      case query: Query.FromQuery[?] =>
        val input   = ToFrom.transform(in)
        val outputs = input.get(query.inputTag).asInstanceOf[Iterable[Out]].iterator
        while (step.check() && outputs.hasNext)
          step.next(outputs.next())

      case query: Query.AliasedQuery[In, ?] =>
        val input   = ToFrom.transform(in)
        val outputs = input.get(query.inputAliasedTag).asInstanceOf[Iterable[Out]].iterator
        while (step.check() && outputs.hasNext)
          step.next(outputs.next())

      case query: Query.Accumulate[In, mid, s, Out] =>
        var state = query.initialState
        interpret[In, mid](in, query.source) {
          Step.always[mid] { elem =>
            state = query.modifyState(state, elem)
          }
        }
        query.getResults(state).foreach(step.next)

      case query: Query.StatefulMapConcat[In, mid, s, Out] =>
        var state = query.initialState
        interpret[In, mid](in, query.source) {
          Step.always[mid] { elem =>
            val (newState, outs) = query.process(state, elem)
            state = newState
            outs.foreach(step.next)
          }
        }

      case query: Query.UnionQuery[In, Out] =>
        import query.*
        interpret[In, Out](in, left)(step)
        interpret[In, Out](in, right)(step)

      case query: Query.AndThenQuery[In, out0, Out] =>
        import query.*
        val tmpBuffer = ListBuffer.empty[out0]
        interpret[In, out0](in, left)(
          Step.always[out0](tmpBuffer += _)
        )
        val input = ToFrom.transform(in) and From.singleTag(outATag, tmpBuffer.toList)
        interpret[From[out0], Out](input.asInstanceOf[From[out0]], right)(step)

      case query: Query.CollectQuery[In, out0, Out] =>
        import query.*
        interpret[In, out0](in, source)(
          Step[out0](
            check = step.check,
            next = out0 => collectFunc.andThen(step.next).applyOrElse[out0, Unit](out0, _ => ())
          )
        )

      case query: Query.WhereQuery[In, Out] =>
        import query.*
        interpret[In, Out](in, source)(
          Step[Out](
            check = step.check,
            next = out0 => if (filterFunc(out0)) step.next(out0)
          )
        )

      case query: Query.MapQuery[In, out0, Out] =>
        import query.*
        interpret[In, out0](in, source)(
          Step[out0](
            check = step.check,
            next = out0 => step.next(project(out0))
          )
        )

      case query: Query.FlatMapQuery[In, out0, Out] =>
        import query.*
        interpret[In, out0](in, source)(
          Step[out0](
            check = step.check,
            next = out => interpret[In, Out](in, projectM(out))(step)
          )
        )

      case query: Query.WhereSubQuery[In, Out] =>
        import query.*
        interpret[In, Out](in, source)(
          Step[Out](
            check = step.check,
            next = out =>
              if (QueryResultRunner.runImpl(predicate(out))(in)) {
                step.next(out)
              }
          )
        )

      case query: Query.SortByQuery[In, Out, by] =>
        import query.*

        val tmpBuffer = ListBuffer.empty[Out]
        interpret[In, Out](in, source)(
          Step.always[Out](tmpBuffer += _)
        )

        val outputs = tmpBuffer.sortBy[by](sortBy)(order.toOrdering).iterator
        while (step.check() && outputs.hasNext)
          step.next(outputs.next())

      case query: Query.AggregateQuery[In, out0, g, out1, Out] =>
        import query.*
        val tmpBuffer = ListBuffer.empty[out0]
        interpret[In, out0](in, source)(
          Step.always[out0](tmpBuffer += _)
        )
        val grouped = tmpBuffer.groupBy(group).iterator
        while (step.check() && grouped.hasNext) {
          val (gr, ys)   = grouped.next()
          val aggregated = agg(gr, AggregationView.create[out0]).apply(ys.toList)
          step.next(tupled(aggregated))
        }

      case query: Query.JoinedQuery[In, out0, out1, Out] =>
        import query.*

        val rightBuffer = ListBuffer.empty[out1]

        interpret[In, out1](in, right)(
          Step.always[out1](rightBuffer += _)
        )

        interpret[In, out0](in, left)(
          Step[out0](
            check = () => step.check(),
            next = { x =>
              query match {
                case query: Query.InnerJoinedQuery[In, out0, out1] =>
                  import query.*
                  joinType match {
                    case Query.InnerJoin =>
                      rightBuffer
                        .filter(on(x, _))
                        .foreach(y => step.next((x, y)))

                    case Query.CrossJoin =>
                      rightBuffer
                        .foreach(y => if (on(x, y)) step.next((x, y)))
                  }

                case query: Query.LeftJoinedQuery[In, out0, out1] =>
                  import query.*
                  val rightValues = rightBuffer
                    .filter(on(x, _))

                  if (rightValues.isEmpty) step.next(x -> None)
                  else rightValues.foreach(y => step.next(x -> Some(y)))
              }
            }
          )
        )
    }
  }
}
