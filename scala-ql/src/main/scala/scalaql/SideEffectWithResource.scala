package scalaql

final case class SideEffectWithResource[R, S, A](
  initialState: S,
  acquire:      () => R,
  release:      (R, S) => Unit,
  use:          (R, S, A) => S) { self =>

  // TODO: think about propagating exceptions
  def beforeAll(f: R => Unit): SideEffectWithResource[R, S, A] =
    copy(
      acquire = () => {
        val resource = self.acquire()
        f(resource)
        resource
      }
    )

  def afterAll(f: (R, S) => Unit): SideEffectWithResource[R, S, A] =
    copy(
      release = { (resource, state) =>
        try
          f(resource, state)
        finally
          self.release(resource, state)
      }
    )

  def onExit(f: => Unit): SideEffectWithResource[R, S, A] =
    copy(
      release = { (resource, state) =>
        self.release(resource, state)
        f
      }
    )
}

object SideEffectWithResource {
  private val noopAcquire: () => Unit         = () => ()
  private val noopRelease: (Any, Any) => Unit = (_, _) => ()

  def noFinalize[A](f: A => Unit): SideEffectWithResource[Unit, Unit, A] =
    new SideEffectWithResource[Unit, Unit, A]((), noopAcquire, noopRelease, (_, _, a) => f(a))

  def stateless[R, A](acquire: () => R, release: R => Unit, use: (R, A) => Unit): SideEffectWithResource[R, Unit, A] =
    SideEffectWithResource(
      initialState = (),
      acquire = acquire,
      release = (r, _) => release(r),
      use = (r, _, a) => use(r, a)
    )
}
