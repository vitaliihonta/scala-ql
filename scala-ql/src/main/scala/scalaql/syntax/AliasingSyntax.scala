package scalaql.syntax

import scalaql.{From, Query}
import scala.annotation.showAsInfix
import scala.language.implicitConversions

trait AliasingSyntax {

  type as[T, U] = scalaql.syntax.as[T, U]

  final implicit def QueryAliasing[In](self: Query[From[In], In]): QueryAliasing[In] =
    new QueryAliasing[In](self)

  final implicit def FromAliasing[A](self: From[A]): FromAliasing[A] =
    new FromAliasing[A](self)
}

/** Actually, there is no boxing. This class is even not instantiated.
  *
  * It's used only because of type widening: shapeless `T with Tag[U]` approach works bad with `From`
  *
  * Original idea by Miles Sabin, see: https://gist.github.com/milessabin/89c9b47a91017973a35f
  */
@showAsInfix
final class as[T, U](private val `dummy`: T) extends AnyVal
