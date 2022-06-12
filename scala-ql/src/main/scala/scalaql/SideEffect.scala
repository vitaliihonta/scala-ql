package scalaql

final case class SideEffect[R, S, A](
  initialState: S,
  acquire:      () => R,
  release:      (R, S) => Unit,
  use:          (R, S, A) => S) { self =>

  // TODO: think about propagating exceptions
  def beforeAll(f: R => Unit): SideEffect[R, S, A] =
    copy(
      acquire = () => {
        val resource = self.acquire()
        f(resource)
        resource
      }
    )

  def afterAll(f: (R, S) => Unit): SideEffect[R, S, A] =
    copy(
      release = { (resource, state) =>
        try
          f(resource, state)
        finally
          self.release(resource, state)
      }
    )

  def onExit(f: => Unit): SideEffect[R, S, A] =
    copy(
      release = { (resource, state) =>
        self.release(resource, state)
        f
      }
    )
}

object SideEffect {
  private val noopAcquire: () => Unit         = () => ()
  private val noopRelease: (Any, Any) => Unit = (_, _) => ()

  type Simple[A]          = SideEffect[Unit, Unit, A]
  type Stateless[R, A]    = SideEffect[R, Unit, A]
  type Resourceless[S, A] = SideEffect[Unit, S, A]

  def simple[A](f: A => Unit): SideEffect.Simple[A] =
    new SideEffect[Unit, Unit, A]((), noopAcquire, noopRelease, (_, _, a) => f(a))

  def resourceless[S, A](initialState: S, use: (S, A) => S): SideEffect.Resourceless[S, A] =
    SideEffect(
      initialState = initialState,
      acquire = noopAcquire,
      release = noopRelease,
      use = (_, s, a) => use(s, a)
    )

  def stateless[R, A](acquire: () => R, release: R => Unit, use: (R, A) => Unit): SideEffect.Stateless[R, A] =
    SideEffect(
      initialState = (),
      acquire = acquire,
      release = (r, _) => release(r),
      use = (r, _, a) => use(r, a)
    )
}
