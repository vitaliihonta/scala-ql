package scalaql.internal

import izumi.reflect.macrortti.LightTypeTag
import scalaql.{Aggregation, From, Query, QueryExpressionBuilder, Ranking, ToFrom}
import scalaql.interpreter.QueryInterpreter

import scala.collection.mutable
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

      case query: Query.OrderByQuery[In, Out, by] =>
        import query.*

        val tmpBuffer = ListBuffer.empty[Out]
        interpret[In, Out](in, source)(
          Step.always[Out](tmpBuffer += _)
        )

        val outputs = tmpBuffer.sortBy[by](orderBy)(ordering).iterator
        while (step.check() && outputs.hasNext)
          step.next(outputs.next())

      case query: Query.AggregateQuery[In, out0, g, aggRes, Out] =>
        import query.*
        type Acc = Any

        val groupedAccs = mutable.Map.empty[g, Acc]
        val aggregate   = agg(QueryExpressionBuilder.create[out0]).asInstanceOf[Aggregation.Aux[out0, Any, aggRes]]

        interpret[In, out0](in, source)(
          Step.always[out0] { mid =>
            val g = group(mid)
            // Populate cache with initial accumulator if not present
            if (!groupedAccs.contains(g)) {
              groupedAccs += (g -> (aggregate.init()))
            }

            // Update the accumulator and put back into cache
            val acc     = groupedAccs(g)
            val updated = aggregate.update(acc, mid)
            groupedAccs.update(g, updated)
          }
        )

        groupedAccs.foreach { case (g, acc) =>
          val res = aggregate.result(acc)
          // Emmit result along with the grouping key(s)
          step.next(flatten((g, res)))
        }

        groupedAccs.clear()

      case query: Query.AggregateRollupQuery[In, out0, aggRes, Out] =>
        import query.*
        type Acc = Any

        // Hash code is more stable then lists
        val accCache = mutable.Map.empty[RollupMapKey, Acc]

        val aggregate = agg(QueryExpressionBuilder.create[out0]).asInstanceOf[Aggregation.Aux[out0, Any, aggRes]]

        interpret[In, out0](in, source)(
          Step.always[out0] { mid =>
            val groupKeys = new RollupMapKey(groups(mid))

            // Starting from all the keys
            var offset = 0
            val end    = groupKeys.size

            // TODO: think how to reuse previous depth accumulation here.
            while (offset <= end) {
              // Get subset of grouping keys.
              // For lists, `drop` is better then `take`
              val subGroup = groupKeys.drop(offset)

              // The map contains accumulators for each possible (sub)group
              if (!accCache.contains(subGroup)) {
                accCache += (subGroup -> aggregate.init())
              }

              // Update the accumulator and put back into cache
              val acc     = accCache(subGroup)
              val updated = aggregate.update(acc, mid)
              accCache.update(subGroup, updated)

              // prepare next iteration
              offset += 1
            }
          }
        )

        // Persist the group size
        val groupKeysNum = rollupGroups.size

        val keysOrdering = new RollupGroupingKeyOrdering(rollupGroups.map(_.ordering))

        // order before emitting
        accCache.toList
          .sortBy { case (k, _) => k }(keysOrdering)
          .foreach { case (currentKeys, acc) =>
            val res = aggregate.result(acc)

            val currentKeysNum = currentKeys.size
            // Fillments for topper-level aggregations
            val fillments = List.tabulate(
              // Basically the number of fillments to provide
              groupKeysNum - currentKeysNum
            )(offset =>
              rollupGroups(
                // In case of top-level aggregation, currentKeysNum is 0, so need to fill each grouping key.
                // In case of mid-level aggregation M, fill N-M keys.
                // In case of bottom-level aggregation, doesn't fill
                currentKeysNum + offset
              ).defaultFill
            )

            val presentKeys = currentKeys.mapWithIndex { (key, i) =>
              // Convert grouping key to it's fillment, e.g. wrap into Some(_) or just return the same value
              rollupGroups(i).groupFill(key)
            }

            // Merge keys before building a tuple.
            val groupKeys = fillments ::: presentKeys
            // Build the tuple
            val output = buildRes(groupKeys ::: List(res))

            // Emmit
            step.next(output)
          }

        accCache.clear()

      case query: Query.WindowQuery[In, out0, res, Out] =>
        import query.*
        val expression = expressionBuilder(QueryExpressionBuilder.create[out0])
        val buffers    = mutable.Map.empty[Int, mutable.PriorityQueue[out0]]
        interpret[In, out0](in, source)(
          Step.always[out0] { value =>
            val partitionKey = window.getPartitionKey(value)
            if (!buffers.contains(partitionKey)) {
              // PriorityQueue pops-up in reverse order =)
              val ordering = window.ordering.reverse
              buffers(partitionKey) = mutable.PriorityQueue.empty[out0](ordering)
            }
            buffers(partitionKey).enqueue(value)
          }
        )

        buffers.foreach { case (_, partition) =>
          expression.processWindow(window.ordering, partition.dequeueAll)(flatten).foreach(step.next)
          partition.clear()
        }

        buffers.clear()

      case query: Query.JoinedQuery[in0, in1, out0, out1, Out] =>
        import query.*

        val rightBuffer = ListBuffer.empty[out1]

        interpret[in1, out1](in.asInstanceOf[in1], right)(
          Step.always[out1](rightBuffer += _)
        )

        interpret[in0, out0](in.asInstanceOf[in0], left)(
          Step[out0](
            check = () => step.check(),
            next = { x =>
              query match {
                case query: Query.InnerJoinedQuery[in0, in1, out0, out1] =>
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

                case query: Query.LeftJoinedQuery[in0, in1, out0, out1] =>
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
