package scalaql.describe

import magnolia1.{CaseClass, Magnolia}
import scala.language.experimental.macros

trait DescribeAutoDerivation {
  type Typeclass[T] = Describe[T]

  def join[T](ctx: CaseClass[Describe, T]): Describe[T] = new Describe[T] {

    override def apply(value: T, visitor: DescribeVisitor)(implicit describeContext: DescribeContext): Unit =
      ctx.parameters.foreach { param =>
        param.typeclass.apply(param.dereference(value), visitor)(
          describeContext.enterField(param.label)
        )
      }
  }

  implicit def autoDerive[T]: Describe[T] = macro Magnolia.gen[T]
}
