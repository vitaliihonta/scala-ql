package scalaql

import scala.annotation.implicitNotFound

@implicitNotFound("Type ${A} is not allowed to be query input: should be Any or From[A] (..with From[B] with ..)")
sealed trait ToFrom[A] {
  def transform(value: A): From[_]
}

object ToFrom {

  def transform[A: ToFrom](value: A): From[_] = implicitly[ToFrom[A]].transform(value)

  implicit def fromToFrom[In <: From[_]]: ToFrom[In] = impl.FromToFrom[In]

  implicit val anyToFrom: ToFrom[Any] = impl.AnyToFrom

  @internalApi
  private final object impl {

    class FromToFrom[In <: From[_]] private () extends ToFrom[In] {
      override def transform(value: In): From[_] = value
    }

    object FromToFrom {
      private val fromToFromInstance: FromToFrom[From[Any]] = new FromToFrom[From[Any]]

      def apply[In <: From[_]]: ToFrom[In] = fromToFromInstance.asInstanceOf[ToFrom[In]]
    }

    object AnyToFrom extends ToFrom[Any] {
      override def transform(value: Any): From[_] = From.empty
    }
  }

}
