package scalaql.syntax

import izumi.reflect.Tag
import scalaql.From
import scalaql.forbiddenInheritance

@forbiddenInheritance
trait ScalaqlDsl {
  final val select: SelectDsl = new SelectDsl()

  final def from[A: Tag](values: Iterable[A]): From[A] = From.single[A](values)

}
