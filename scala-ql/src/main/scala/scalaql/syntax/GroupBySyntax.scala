package scalaql.syntax

import scalaql.*
import scalaql.internal.GroupBySyntaxMacro
import scala.language.experimental.macros

final class GroupBySyntax[In, Out](@internalApi val self: Query[In, Out]) extends AnyVal {

  // TODO: Add more arity
  // TODO: Implement for scala3
  def groupBy[A](f: Out => A): GroupedQuery1[In, Out, A] =
    macro GroupBySyntaxMacro.groupBy1Impl[In, Out, A]

//  /**
//   * Entrypoint for performing aggregations on this query.
//   *
//   * @tparam A the first grouping key type
//   * @tparam B the second grouping key type
//   * @param f1 extracts the first grouping key
//   * @param f2 extracts the second grouping key
//   * */
//  def groupBy[A: Tag, B: Tag](
//    f1:          Out => A,
//    f2:          Out => B
//  )(implicit In: Tag[In],
//    Out:         Tag[Out]
//  ): GroupedQuery2[In, Out, A, B] =
//    new GroupedQuery2[In, Out, A, B](
//      self,
//      f1,
//      f2,
//      Query.GroupKind.Simple[A],
//      Query.GroupKind.Simple[B]
//    )
//
//  /**
//   * Entrypoint for performing aggregations on this query.
//   *
//   * @tparam A the first grouping key type
//   * @tparam B the second grouping key type
//   * @tparam C the third grouping key type
//   * @param f1 extracts the first grouping key
//   * @param f2 extracts the second grouping key
//   * @param f3 extracts the third grouping key
//   * */
//  def groupBy[A: Tag, B: Tag, C: Tag](
//    f1:          Out => A,
//    f2:          Out => B,
//    f3:          Out => C
//  )(implicit In: Tag[In],
//    Out:         Tag[Out]
//  ): GroupedQuery3[In, Out, A, B, C] =
//    new GroupedQuery3[In, Out, A, B, C](self, f1, f2, f3)
}
