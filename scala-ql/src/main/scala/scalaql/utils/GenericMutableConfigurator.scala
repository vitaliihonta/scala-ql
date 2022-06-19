package scalaql.utils

class GenericMutableConfigurator[C, A] private (build: (C, A) => Unit) extends ((C, A) => Unit) {
  override def apply(c: C, v1: A): Unit = build(c, v1)

  def andThen(f: A => Unit): GenericMutableConfigurator[C, A] =
    new GenericMutableConfigurator[C, A]((c: C, a: A) => {
      build(c, a)
      f(a)
    })
}

object GenericMutableConfigurator {
  def apply[A](): GenericMutableConfigurator[Unit, A] = new GenericMutableConfigurator[Unit, A]((_, _) => {})

  def withContext[C, A]: GenericMutableConfigurator[C, A] = new GenericMutableConfigurator[C, A]((_, _) => {})
}
