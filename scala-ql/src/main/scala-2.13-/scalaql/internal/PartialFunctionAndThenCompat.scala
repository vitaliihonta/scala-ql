package scalaql.internal

// In Scala 2.12, there is no `andThen` which accepts PartialFunction.
// So in that case, PartialFunction will be downcasted to just Function, causing MatchError
object PartialFunctionAndThenCompat {
  def andThen[A, B, C](pf1: PartialFunction[A, B], pf2: PartialFunction[B, C]): PartialFunction[A, C] =
    new Combined(pf1, pf2)

  private[this] val fallback_fn: Any => Any = _ => fallback_fn
  private def checkFallback[B]              = fallback_fn.asInstanceOf[Any => B]
  private def fallbackOccurred[B](x: B)     = fallback_fn eq x.asInstanceOf[AnyRef]

  // Copy-pasted from Scala 2.13
  private class Combined[-A, B, +C](pf: PartialFunction[A, B], k: PartialFunction[B, C])
      extends PartialFunction[A, C]
      with Serializable {
    def isDefinedAt(x: A): Boolean = {
      val b: B = pf.applyOrElse(x, checkFallback[B])
      if (!fallbackOccurred(b)) k.isDefinedAt(b) else false
    }

    def apply(x: A): C = k(pf(x))

    override def applyOrElse[A1 <: A, C1 >: C](x: A1, default: A1 => C1): C1 = {
      val pfv = pf.applyOrElse(x, checkFallback[B])
      if (!fallbackOccurred(pfv)) k.applyOrElse(pfv, (_: B) => default(x)) else default(x)
    }
  }
}
