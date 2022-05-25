package scalaql.internal

case class Step[A](check: () => Step.Result, next: A => Unit)

object Step {
  type Result = Boolean
  val Next: Result = true
  val Stop: Result = false

  private val checkAlwaysNext: () => Result = () => Next

  def always[A](next: A => Unit): Step[A] = Step(checkAlwaysNext, next)
}
