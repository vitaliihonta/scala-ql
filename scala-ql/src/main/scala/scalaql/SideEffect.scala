package scalaql

final class SideEffect[R, S, A] private (
  private var state:             S,
  private var acquireResource:   () => R,
  private var releaseResource:   (R, S) => Unit,
  private var useResource:       (R, S, A) => S,
  private var capturedException: Throwable) { self =>

  def acquire(): R               = acquireResource()
  def release(resource: R): Unit = releaseResource(resource, state)

  def use(resource: R, value: A): this.type =
    if (capturedException != null) {
      throw capturedException
    } else
      try {
        val newState = useResource(resource, state, value)
        self.state = newState
        self
      } catch {
        case e: Throwable =>
          capturedException = e
          self
      }

  def beforeAll(f: R => Unit): this.type = {
    val previousAcquire = self.acquireResource
    self.acquireResource = () => {
      val resource = previousAcquire()
      try f(resource)
      catch {
        case e: Throwable => self.capturedException = e
      }
      resource
    }
    self
  }

  def afterAll(f: (R, S) => Unit): this.type = {
    val previousRelease = self.releaseResource
    self.releaseResource = { (resource, state) =>
      try
        if (self.capturedException == null) f(resource, state)
      finally
        previousRelease(resource, state)
    }
    self
  }

  def onExit(f: => Unit): this.type = {
    val previousRelease = self.releaseResource
    self.releaseResource = { (resource, state) =>
      previousRelease(resource, state)
      f
    }
    self
  }
}

object SideEffect {
  private val noopAcquire: () => Unit         = () => ()
  private val noopRelease: (Any, Any) => Unit = (_, _) => ()

  type Simple[A]          = SideEffect[Unit, Unit, A]
  type Stateless[R, A]    = SideEffect[R, Unit, A]
  type Resourceless[S, A] = SideEffect[Unit, S, A]

  def apply[R, S, A](
    initialState: S,
    acquire:      () => R,
    release:      (R, S) => Unit,
    use:          (R, S, A) => S
  ): SideEffect[R, S, A] =
    new SideEffect[R, S, A](
      state = initialState,
      acquireResource = acquire,
      releaseResource = release,
      useResource = use,
      capturedException = null
    )

  def simple[A](f: A => Unit): SideEffect.Simple[A] =
    new SideEffect[Unit, Unit, A](
      state = (),
      acquireResource = noopAcquire,
      releaseResource = noopRelease,
      useResource = (_, _, a) => f(a),
      capturedException = null
    )

  def resourceless[S, A](initialState: S, use: (S, A) => S): SideEffect.Resourceless[S, A] =
    new SideEffect[Unit, S, A](
      state = initialState,
      acquireResource = noopAcquire,
      releaseResource = noopRelease,
      useResource = (_, s, a) => use(s, a),
      capturedException = null
    )

  def stateless[R, A](acquire: () => R, release: R => Unit, use: (R, A) => Unit): SideEffect.Stateless[R, A] =
    new SideEffect[R, Unit, A](
      state = (),
      acquireResource = acquire,
      releaseResource = (r, _) => release(r),
      useResource = (r, _, a) => use(r, a),
      capturedException = null
    )
}
