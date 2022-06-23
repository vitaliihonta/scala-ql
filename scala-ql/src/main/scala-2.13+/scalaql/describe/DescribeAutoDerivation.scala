package scalaql.describe

import language.experimental.macros
import magnolia1.*

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
