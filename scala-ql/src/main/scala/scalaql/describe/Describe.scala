package scalaql.describe

import scalaql.sources.columnar.{CodecPath, TableApiWriteContext}

case class DescribeContext(
  location: CodecPath,
  headers:  List[String])
    extends TableApiWriteContext[DescribeContext] { self =>

  override def enterField(name: String): DescribeContext =
    copy(location = CodecPath.AtField(name, self.location))

  override def enterIndex(idx: Int): DescribeContext =
    copy(location = CodecPath.AtIndex(idx, self.location.fieldLocation))
}

object DescribeContext {
  def initial: DescribeContext = DescribeContext(CodecPath.Root, Nil)
}

trait Describe[A] {
  def apply(value: A, visitor: DescribeVisitor)(implicit ctx: DescribeContext): Unit
}

object Describe extends DescribeAutoDerivation with LowPriorityDescribeInstances {
  def apply[A](implicit ev: Describe[A]): ev.type = ev
}

trait LowPriorityDescribeInstances {
  def describeAsBigDecimal[A](toBigDecimal: A => BigDecimal): Describe[A] =
    new DescribeAsDecimal[A](toBigDecimal)

  implicit val describeString: Describe[String]   = new DescribeAny[String]
  implicit val describeBoolean: Describe[Boolean] = new DescribeAny[Boolean]
  implicit val describeInt: Describe[Int]         = describeAsBigDecimal[Int](BigDecimal(_))
  implicit val describeLong: Describe[Long]       = describeAsBigDecimal[Long](BigDecimal(_))
  implicit val describeDouble: Describe[Double]   = describeAsBigDecimal[Double](BigDecimal(_))
  implicit val describeBigInt: Describe[BigInt]   = describeAsBigDecimal[BigInt](BigDecimal(_))

  implicit val describeBigDecimal: Describe[BigDecimal] =
    describeAsBigDecimal(identity[BigDecimal])

  implicit def describeOption[A: Describe]: Describe[Option[A]] =
    new Describe[Option[A]] {
      override def apply(value: Option[A], visitor: DescribeVisitor)(implicit ctx: DescribeContext): Unit =
        value.foreach { value =>
          Describe[A].apply(value, visitor)
        }
    }

  implicit def describeIterable[Coll[x] <: Iterable[x], A: Describe]: Describe[Coll[A]] =
    new Describe[Coll[A]] {
      override def apply(value: Coll[A], visitor: DescribeVisitor)(implicit ctx: DescribeContext): Unit =
        value.zipWithIndex.foreach { case (value, idx) =>
          Describe[A].apply(value, visitor)(
            ctx.enterIndex(idx)
          )
        }
    }
}

private class DescribeAsDecimal[A](toBigDecimal: A => BigDecimal) extends Describe[A] {
  override def apply(value: A, visitor: DescribeVisitor)(implicit ctx: DescribeContext): Unit =
    visitor.addNumeric(ctx.fieldLocation.name, toBigDecimal(value))
}

private class DescribeAny[A] extends Describe[A] {
  override def apply(value: A, visitor: DescribeVisitor)(implicit ctx: DescribeContext): Unit =
    visitor.addCount(ctx.fieldLocation.name, 1)
}
