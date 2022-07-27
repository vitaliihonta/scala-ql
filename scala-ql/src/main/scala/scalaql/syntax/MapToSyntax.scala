package scalaql.syntax

import scalaql.*
import scala.language.implicitConversions

trait MapToSyntax {
  implicit def MapToSyntax2[In, A, B](self: Query[In, (A, B)]): MapToSyntax2[In, A, B] =
    new MapToSyntax2[In, A, B](self)

  implicit def MapToSyntax3[In, A, B, C](self: Query[In, (A, B, C)]): MapToSyntax3[In, A, B, C] =
    new MapToSyntax3[In, A, B, C](self)

  implicit def MapToSyntax4[In, A, B, C, D](self: Query[In, (A, B, C, D)]): MapToSyntax4[In, A, B, C, D] =
    new MapToSyntax4[In, A, B, C, D](self)

  implicit def MapToSyntax5[In, A, B, C, D, E](self: Query[In, (A, B, C, D, E)]): MapToSyntax5[In, A, B, C, D, E] =
    new MapToSyntax5[In, A, B, C, D, E](self)

  implicit def MapToSyntax6[In, A, B, C, D, E, F](
    self: Query[In, (A, B, C, D, E, F)]
  ): MapToSyntax6[In, A, B, C, D, E, F] =
    new MapToSyntax6[In, A, B, C, D, E, F](self)
}

final class MapToSyntax2[In, A, B](private val self: Query[In, (A, B)]) extends AnyVal {
  def mapTo[Res: Tag](f: (A, B) => Res): Query[In, Res] =
    self.map(f.tupled)
}

final class MapToSyntax3[In, A, B, C](private val self: Query[In, (A, B, C)]) extends AnyVal {
  def mapTo[Res: Tag](f: (A, B, C) => Res): Query[In, Res] =
    self.map(f.tupled)
}

final class MapToSyntax4[In, A, B, C, D](private val self: Query[In, (A, B, C, D)]) extends AnyVal {
  def mapTo[Res: Tag](f: (A, B, C, D) => Res): Query[In, Res] =
    self.map(f.tupled)
}

final class MapToSyntax5[In, A, B, C, D, E](private val self: Query[In, (A, B, C, D, E)]) extends AnyVal {
  def mapTo[Res: Tag](f: (A, B, C, D, E) => Res): Query[In, Res] =
    self.map(f.tupled)
}

final class MapToSyntax6[In, A, B, C, D, E, F](private val self: Query[In, (A, B, C, D, E, F)]) extends AnyVal {
  def mapTo[Res: Tag](f: (A, B, C, D, E, F) => Res): Query[In, Res] =
    self.map(f.tupled)
}
