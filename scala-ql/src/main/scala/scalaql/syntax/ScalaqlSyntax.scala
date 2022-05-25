package scalaql.syntax

import scalaql._

@forbiddenInheritance
trait ScalaqlSyntax extends ScalaqlAliases with ScalaqlDsl {

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
}
