package scalaql.describe

import scala.annotation.implicitNotFound

@implicitNotFound("Don't know how to convert ${A} to BigDecimal.")
trait ToBigDecimal[A] extends Serializable {
  def apply(value: A): BigDecimal
}

object ToBigDecimal {
  def apply[A](implicit ev: ToBigDecimal[A]): ev.type = ev

  def create[A](f: A => BigDecimal): ToBigDecimal[A] = (value: A) => f(value)

  implicit val IntToBD: ToBigDecimal[Int]               = create[Int](BigDecimal(_))
  implicit val LongToBD: ToBigDecimal[Long]             = create[Long](BigDecimal(_))
  implicit val DoubleToBD: ToBigDecimal[Double]         = create[Double](BigDecimal(_))
  implicit val BigIntToBD: ToBigDecimal[BigInt]         = create[BigInt](BigDecimal(_))
  implicit val BigDecimalToBD: ToBigDecimal[BigDecimal] = create[BigDecimal](identity)
}
