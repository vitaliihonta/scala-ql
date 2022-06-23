package scalaql.describe

import scala.deriving.Mirror
import magnolia1.*

trait DescribeAutoDerivation extends ProductDerivation[Describe] {

  def join[T](ctx: CaseClass[Describe, T]): Describe[T] = new Describe[T] {

    override def apply(value: T, visitor: DescribeVisitor)(implicit describeContext: DescribeContext): Unit =
      ctx.params.foreach { param =>
        param.typeclass.apply(param.deref(value), visitor)(
          describeContext.enterField(param.label)
        )
      }
  }

  inline given autoDerive[T](using Mirror.Of[T]): Describe[T] = derived[T]
}
