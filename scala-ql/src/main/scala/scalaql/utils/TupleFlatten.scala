package scalaql.utils

import scala.annotation.implicitNotFound

@implicitNotFound("Don't now how to flatten tuple ${A}")
sealed trait TupleFlatten[-A] {
  type Out

  def apply(x: A): Out
}

object TupleFlatten extends LowPriorityTupled0 {
  type Aux[A, Out0] = TupleFlatten[A] { type Out = Out0 }

  private[scalaql] def create[A, Out0](f: A => Out0): TupleFlatten.Aux[A, Out0] =
    new TupleFlatten[A] {
      type Out = Out0

      override def apply(x: A): Out = f(x)
    }

  implicit def tupled2Plus[A, B, C]: TupleFlatten.Aux[((A, B), C), (A, B, C)] =
    create[((A, B), C), (A, B, C)] { case ((a, b), c) =>
      (a, b, c)
    }

  implicit def tupledPlus2[A, B, C]: TupleFlatten.Aux[(A, (B, C)), (A, B, C)] =
    create[(A, (B, C)), (A, B, C)] { case (a, (b, c)) =>
      (a, b, c)
    }

  implicit def tupled3Plus[A, B, C, D]: TupleFlatten.Aux[((A, B, C), D), (A, B, C, D)] =
    create[((A, B, C), D), (A, B, C, D)] { case ((a, b, c), d) =>
      (a, b, c, d)
    }

  implicit def tupledPlus3[A, B, C, D]: TupleFlatten.Aux[(A, (B, C, D)), (A, B, C, D)] =
    create[(A, (B, C, D)), (A, B, C, D)] { case (a, (b, c, d)) =>
      (a, b, c, d)
    }

  // todo: make up to 7
}

sealed trait LowPriorityTupled0 {
  implicit def tupled0[A]: TupleFlatten.Aux[A, A] = TupleFlatten.create[A, A](identity)

}
