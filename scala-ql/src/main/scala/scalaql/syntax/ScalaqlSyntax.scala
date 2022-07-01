package scalaql.syntax

import scalaql.*
import scalaql.describe.Describe
import scalaql.utils.EachSyntax
import scalaql.visualization.ShowAsTable
import scala.language.implicitConversions

@forbiddenInheritance
trait ScalaqlSyntax extends ScalaqlAliases with ScalaqlDsl with OrderingSyntax with AliasingSyntax with EachSyntax {

  final implicit def RunSyntax[In, Out](self: QueryResult[In, Out]): RunSyntax[In, Out] =
    new RunSyntax[In, Out](self)

  final implicit def RunSyntaxAny[Out](self: QueryResult[Any, Out]): RunSyntaxAny[Out] =
    new RunSyntaxAny[Out](self)

  final implicit def BasicQuerySyntax[In, Out](self: Query[In, Out]): BasicQuerySyntax[In, Out] =
    new BasicQuerySyntax[In, Out](self)

  final implicit def MapQuerySyntax[In, K, V](self: Query[In, (K, V)]): MapQuerySyntax[In, K, V] =
    new MapQuerySyntax[In, K, V](self)

  final implicit def QueryToSyntax[In, Out](self: Query[In, Out]): QueryToSyntax[In, Out] =
    new QueryToSyntax[In, Out](self)

  final implicit def WhereSyntax[A](self: A): WhereSyntax[A] =
    new WhereSyntax[A](self)

  final implicit def ShowSyntax[In, Out: ShowAsTable](self: Query[In, Out]): ShowSyntax[In, Out] =
    new ShowSyntax[In, Out](self)

  final implicit def DescribeSyntax[In, Out: Describe](self: Query[In, Out]): DescribeSyntax[In, Out] =
    new DescribeSyntax[In, Out](self)

  final implicit def SortingSyntax[In, Out](self: Query[In, Out]): SortingSyntax[In, Out] =
    new SortingSyntax[In, Out](self)

  final implicit def JoinSyntax[In <: From[?], Out](self: Query[In, Out]): JoinSyntax[In, Out] =
    new JoinSyntax[In, Out](self)
}
