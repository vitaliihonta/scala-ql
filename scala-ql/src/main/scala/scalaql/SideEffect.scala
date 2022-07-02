package scalaql

import scala.collection.mutable

final class SideEffect[R, S, A] private (
  private var state:             S,
  private var acquireResource:   () => R,
  private var releaseResource:   (R, S) => Unit,
  private var useResource:       (R, S, A) => S,
  private val beforeAlls:        mutable.ListBuffer[R => Unit],
  private val afterAlls:         mutable.ListBuffer[(R, S) => Unit],
  private val onExits:           mutable.ListBuffer[() => Unit],
  private var capturedException: Throwable)
    extends AutoCloseable
    with Serializable
    with (A => Unit) { self =>

  private lazy val resource = {
    val res = acquireResource()
    try
      beforeAlls.foreach(_.apply(res))
    catch {
      case e: Throwable => capturedException = e
    }
    res
  }

  override def close(): Unit =
    try
      if (capturedException == null) afterAlls.foreach(_.apply(resource, state))
    finally {
      releaseResource(resource, state)
      onExits.foreach(_.apply())
    }

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
    self.beforeAlls += f
    self
  }

  def afterAll(f: (R, S) => Unit): this.type = {
    afterAlls += f
    self
  }

  def onExit(f: => Unit): this.type = {
    self.onExits += (() => f)
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
      capturedException = null,
      beforeAlls = mutable.ListBuffer.empty,
      afterAlls = mutable.ListBuffer.empty,
      onExits = mutable.ListBuffer.empty
    )

  def resourceless[S, A](initialState: S, use: (S, A) => S): SideEffect.Resourceless[S, A] =
    new SideEffect[Unit, S, A](
      state = initialState,
      acquireResource = noopAcquire,
      releaseResource = noopRelease,
      useResource = (_, s, a) => use(s, a),
      capturedException = null,
      beforeAlls = mutable.ListBuffer.empty,
      afterAlls = mutable.ListBuffer.empty,
      onExits = mutable.ListBuffer.empty
    )

  def stateless[R, A](acquire: () => R, release: R => Unit, use: (R, A) => Unit): SideEffect.Stateless[R, A] =
    new SideEffect[R, Unit, A](
      state = (),
      acquireResource = acquire,
      releaseResource = (r, _) => release(r),
      useResource = (r, _, a) => use(r, a),
      capturedException = null,
      beforeAlls = mutable.ListBuffer.empty,
      afterAlls = mutable.ListBuffer.empty,
      onExits = mutable.ListBuffer.empty
    )
}
