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
        while (step.check() && outputs.hasNext) {
          step.next(outputs.next())
        }

      case query: Query.FromQuery[?] =>
        val input   = ToFrom.transform(in)
        val outputs = input.get(query.inputTag).asInstanceOf[Iterable[Out]].iterator
        while (step.check() && outputs.hasNext) {
          step.next(outputs.next())
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

      case query: Query.MapFilterQuery[In, out0, Out] =>
        import query.*
        interpret[In, out0](in, source)(
          Step[out0](
            check = step.check,
            next = out0 => mapFilterFunc(out0).foreach(step.next)
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
        while (step.check() && outputs.hasNext) {
          step.next(outputs.next())
        }

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
          step.next(tupled(gr -> aggregated))
        }

      case query: Query.JoinedQuery[In, out0, out1, Out] =>
        import query.*
        val leftBuffer  = ListBuffer.empty[out0]
        val rightBuffer = ListBuffer.empty[out1]

        interpret[In, out0](in, left)(
          Step.always[out0](leftBuffer += _)
        )
        interpret[In, out1](in, right)(
          Step.always[out1](rightBuffer += _)
        )

        // todo: optimize join
        val leftValues = leftBuffer.iterator
        while (step.check() && leftValues.hasNext) {
          val x = leftValues.next()
          query match {
            case query: Query.InnerJoinedQuery[In, out0, out1] =>
              import query.*
              joinType match {
                case Query.InnerJoin =>
                  rightBuffer
                    .find(on(x, _))
                    .foreach(y => step.next((x, y)))

                case Query.CrossJoin =>
                  rightBuffer
                    .foreach(y => if (on(x, y)) step.next((x, y)))
              }

            case query: Query.LeftJoinedQuery[In, out0, out1] =>
              import query.*
              step.next {
                x -> rightBuffer
                  .find(on(x, _))
              }
          }
        }
    }
  }
}
