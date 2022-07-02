package scalaql.utils

import scalaql.Tag
import scala.annotation.implicitNotFound

@implicitNotFound("Don't now how to flatten tuple ${A}")
sealed trait TupleFlatten[-A] {
  type Out

  def apply(x: A): Out

  def tag: Tag[Out]
}

object TupleFlatten extends LowPriorityTupled0 {
  type Aux[A, Out0] = TupleFlatten[A] { type Out = Out0 }

  private[scalaql] def create[A, Out0: Tag](f: A => Out0): TupleFlatten.Aux[A, Out0] =
    new TupleFlatten[A] {
      type Out = Out0

      override def apply(x: A): Out = f(x)

      override val tag: Tag[Out0] = Tag[Out0]
    }

  implicit def tupled2[A: Tag, B: Tag, C: Tag, D: Tag]: TupleFlatten.Aux[((A, B), (C, D)), (A, B, C, D)] =
    TupleFlatten.create[((A, B), (C, D)), (A, B, C, D)] { case ((a, b), (c, d)) =>
      (a, b, c, d)
    }

  implicit def tupled2Plus3[
    A: Tag,
    B: Tag,
    C: Tag,
    D: Tag,
    E: Tag
  ]: TupleFlatten.Aux[((A, B), (C, D, E)), (A, B, C, D, E)] =
    TupleFlatten.create[((A, B), (C, D, E)), (A, B, C, D, E)] { case ((a, b), (c, d, e)) =>
      (a, b, c, d, e)
    }

  implicit def tupled3Plus2[
    A: Tag,
    B: Tag,
    C: Tag,
    D: Tag,
    E: Tag
  ]: TupleFlatten.Aux[((A, B, C), (D, E)), (A, B, C, D, E)] =
    TupleFlatten.create[((A, B, C), (D, E)), (A, B, C, D, E)] { case ((a, b, c), (d, e)) =>
      (a, b, c, d, e)
    }

  implicit def tupled3[
    A: Tag,
    B: Tag,
    C: Tag,
    D: Tag,
    E: Tag,
    F: Tag
  ]: TupleFlatten.Aux[((A, B, C), (D, E, F)), (A, B, C, D, E, F)] =
    TupleFlatten.create[((A, B, C), (D, E, F)), (A, B, C, D, E, F)] { case ((a, b, c), (d, e, f)) =>
      (a, b, c, d, e, f)
    }

  implicit def tupled4Plus1[
    A: Tag,
    B: Tag,
    C: Tag,
    D: Tag,
    E: Tag
  ]: TupleFlatten.Aux[((A, B, C, D), E), (A, B, C, D, E)] =
    TupleFlatten.create[((A, B, C, D), E), (A, B, C, D, E)] { case ((a, b, c, d), e) =>
      (a, b, c, d, e)
    }
  // todo: make up to 7
}

sealed trait LowPriorityTupled0 extends LowPriorityTupled1 {
  implicit def tupled2Plus[A: Tag, B: Tag, C: Tag]: TupleFlatten.Aux[((A, B), C), (A, B, C)] =
    TupleFlatten.create[((A, B), C), (A, B, C)] { case ((a, b), c) =>
      (a, b, c)
    }

  implicit def tupledPlus2[A: Tag, B: Tag, C: Tag]: TupleFlatten.Aux[(A, (B, C)), (A, B, C)] =
    TupleFlatten.create[(A, (B, C)), (A, B, C)] { case (a, (b, c)) =>
      (a, b, c)
    }

  implicit def tupled3Plus[A: Tag, B: Tag, C: Tag, D: Tag]: TupleFlatten.Aux[((A, B, C), D), (A, B, C, D)] =
    TupleFlatten.create[((A, B, C), D), (A, B, C, D)] { case ((a, b, c), d) =>
      (a, b, c, d)
    }

  implicit def tupledPlus3[A: Tag, B: Tag, C: Tag, D: Tag]: TupleFlatten.Aux[(A, (B, C, D)), (A, B, C, D)] =
    TupleFlatten.create[(A, (B, C, D)), (A, B, C, D)] { case (a, (b, c, d)) =>
      (a, b, c, d)
    }
}

sealed trait LowPriorityTupled1 {
  implicit def tupled0[A: Tag]: TupleFlatten.Aux[A, A] = TupleFlatten.create[A, A](identity)
}
