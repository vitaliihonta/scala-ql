package scalaql.describe

import scalaql.sources.columnar.{CodecPath, TableApiWriteContext}
import spire.math.Fractional
import spire.implicits.*

import java.time.{LocalDate, LocalDateTime}

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

trait Describe[A] { self =>
  def apply(value: A, visitor: DescribeVisitor)(implicit ctx: DescribeContext): Unit

  def contramap[B](f: B => A): Describe[B] = new Describe[B] {
    override def apply(value: B, visitor: DescribeVisitor)(implicit ctx: DescribeContext): Unit =
      self.apply(f(value), visitor)
  }
}

object Describe extends DescribeAutoDerivation with LowPriorityDescribeInstances {
  def apply[A](implicit ev: Describe[A]): ev.type = ev
}

trait LowPriorityDescribeInstances extends AdditionalDescribeImplicits {

  implicit val describeString: Describe[String]   = new DescribeAny[String]
  implicit val describeBoolean: Describe[Boolean] = new DescribeAny[Boolean]

  implicit val describeDouble: Describe[Double] = new DescribeFractional[Double]

  implicit val describeBigDecimal: Describe[BigDecimal] = new DescribeFractional[BigDecimal]
  implicit val describeInt: Describe[Int]               = describeDouble.contramap(_.toDouble)
  implicit val describeLong: Describe[Long]             = describeDouble.contramap(_.toDouble)
  implicit val describeBigInt: Describe[BigInt]         = describeBigDecimal.contramap(_.toBigDecimal)

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

private class DescribeOrdered[A: Ordering] extends Describe[A] {
  override def apply(value: A, visitor: DescribeVisitor)(implicit ctx: DescribeContext): Unit =
    visitor.addOrdered(ctx.fieldLocation.name, value)
}

private class DescribeFractional[A: Fractional: ToBigDecimal] extends Describe[A] {
  override def apply(value: A, visitor: DescribeVisitor)(implicit ctx: DescribeContext): Unit =
    visitor.addNumeric[A](ctx.fieldLocation.name, value)
}

private class DescribeAny[A] extends Describe[A] {
  override def apply(value: A, visitor: DescribeVisitor)(implicit ctx: DescribeContext): Unit =
    visitor.addNonNumeric(ctx.fieldLocation.name, value)
}
