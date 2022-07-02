package scalaql

import scala.annotation.implicitNotFound

@implicitNotFound("Type ${A} is not allowed to be query input: should be Any or From[A] (..with From[B] with ..)")
sealed trait ToFrom[A] extends Serializable {
  def transform(value: A): From[?]
}

object ToFrom {

  def transform[A: ToFrom](value: A): From[?] = implicitly[ToFrom[A]].transform(value)

  implicit def fromToFrom[In <: From[?]]: ToFrom[In] = impl.FromToFrom[In]

  implicit val anyToFrom: ToFrom[Any] = impl.AnyToFrom

  @internalApi
  private object impl {

    class FromToFrom[In <: From[?]] private () extends ToFrom[In] {
      override def transform(value: In): From[?] = value
    }

    object FromToFrom {
      private val fromToFromInstance: FromToFrom[From[Any]] = new FromToFrom[From[Any]]

      def apply[In <: From[?]]: ToFrom[In] = fromToFromInstance.asInstanceOf[ToFrom[In]]
    }

    object AnyToFrom extends ToFrom[Any] {
      override def transform(value: Any): From[?] = From.empty
    }
  }

}
