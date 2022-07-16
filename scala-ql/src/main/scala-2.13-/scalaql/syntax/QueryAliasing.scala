package scalaql.syntax

import scalaql.Tag
import scalaql.{From, Query}

final class QueryAliasing[In](private val self: Query[From[In], In]) extends AnyVal {

  /**
   * Allows to alias `this` query input type, so that it could be combined with other queries with the same underlying type.
   *
   * @note scala 2.12 doesn't have literal types, so you should use a custom `trait` for aliasing.
   * Example:
   * {{{
   *   sealed trait people1
   *   sealed trait people2
   *
   *   val peers = select[Person]
   *     .as[people1]
   *     .join(select[Person].as[people2])
   *     .on(_.age == _.age)
   *     .map { case (left, right) =>
   *       Peer(
   *         who = left.name,
   *         age = left.age,
   *         peer = right.name
   *       )
   *     }
   * }}}
   *
   * @tparam U type of the alias
   * @return `this` query aliased
   * */
  def as[U](implicit In: Tag[In], U: Tag[U]): Query[From[In as U], In] =
    new Query.AliasedQuery[In, U](
      In.tag,
      Tag[In as U].tag,
      Tag[U].tag
    )
}

final class FromAliasing[A](private val self: From[A]) extends AnyVal {

  /**
   * Allows to alias `this` query input values, so that it could be provided to an aliased query.
   *
   * @note scala 2.12 doesn't have literal types, so you should use a custom `trait` for aliasing.
   * Example:
   * {{{
   *   sealed trait people1
   *   sealed trait people2
   *
   *   val peers: Query[From[Person as people1] with From[Person as people2]] = ???
   *
   *   val actualResult = peers.toList.run(
   *     from(people1).as[people1] &
   *       from(people2).as[people2]
   *   )
   * }}}
   *
   * @tparam U type of the alias
   * @return `this` query input aliased
   * */
  def as[U](implicit A: Tag[A], U: Tag[U]): From[A as U] = {
    val restInputs = self.inputs.filterNot { case (tag, _) => tag == A.tag }
    new From[A as U](
      restInputs.updated(Tag[A as U].tag, self.get(A.tag))
    )
  }
}
