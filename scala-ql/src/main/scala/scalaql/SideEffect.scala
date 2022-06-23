package scalaql

final class SideEffect[R, S, A] private (
  private var state:             S,
  private var acquireResource:   () => R,
  private var releaseResource:   (R, S) => Unit,
  private var useResource:       (R, S, A) => S,
  private var capturedException: Throwable)
    extends AutoCloseable
    with (A => Unit) { self =>

  private lazy val resource = acquireResource()

  override def close(): Unit = releaseResource(resource, state)

  override def apply(value: A): Unit = {
    val _ = resource // touch the resource to capture initialization error
    if (capturedException != null) {
      throw capturedException
    } else {
      try
        self.state = useResource(resource, state, value)
      catch {
        case e: Throwable =>
          self.capturedException = e
          throw e
      }
    }
  }

  def beforeAll(f: R => Unit): this.type = {
    val previousAcquire = self.acquireResource
    self.acquireResource = () => {
      val r = previousAcquire()
      try f(r)
      catch {
        case e: Throwable =>
          self.capturedException = e
      }
      r
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
