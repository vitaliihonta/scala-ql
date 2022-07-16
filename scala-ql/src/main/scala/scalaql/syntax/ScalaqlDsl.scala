package scalaql.syntax

import scalaql.Tag
import scalaql.From
import scalaql.forbiddenInheritance

@forbiddenInheritance
trait ScalaqlDsl {

  /**
   * Entrypoint for building queries.
   *
   * It is allowed to define a query based on:
   *
   * 1. Its input type, so that input values will be provided later:
   * {{{
   *   val query = select[Person]
   * }}}
   *
   * 2. Existing scala collection:
   * {{{
   *   val people: List[Person] = ???
   *
   *   val query = select.from(people)
   * }}}
   *
   * */
  final val select: SelectDsl = new SelectDsl()

  /**
   * Wraps the specified scala collection into a query input type.
   *
   * Example:
   * {{{
   *   val query: Query[From[Person], PeopleStatistics] = ???
   *
   *   val people: List[Person] = ???
   *
   *   query
   *     .show()
   *     .run(
   *       from(people)
   *     )
   * }}}
   *
   * @tparam A the input type
   * @param values input values
   * @return a value which could be then used as a query input
   * */
  final def from[A: Tag](values: Iterable[A]): From[A] = From.single[A](values)

}
