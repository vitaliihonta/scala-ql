package scalaql.internal

import scalaql.internalApi
import scala.collection.mutable.ListBuffer

@internalApi
trait FunctionK[F[_], G[_]] {
  def apply[A](fa: F[A]): G[A]
}

@internalApi
object FunctionK {

  def create[F[_], G[_]](f: F[Any] => G[Any]): FunctionK[F, G] = new FunctionK[F, G] {
    override def apply[A](fa: F[A]): G[A] = f(fa.asInstanceOf[F[Any]]).asInstanceOf[G[A]]
  }

  def identity[F[_]]: FunctionK[F, F] = create[F, F](fa => fa)

  val listBufferToList: FunctionK[ListBuffer, List] = FunctionK.create[ListBuffer, List](_.toList)

  val listBufferToSet: FunctionK[ListBuffer, Set] = FunctionK.create[ListBuffer, Set](_.toSet)
}
