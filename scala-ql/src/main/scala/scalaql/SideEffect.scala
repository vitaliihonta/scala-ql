package scalaql

import scala.collection.mutable

/**
 * Represents a stateful side effect with resource management.
 * Used by `scalaql` to implement readers and writers.
 *
 * @tparam R the resource type
 * @tparam S the state type
 * @tparam A side effect input type
 * */
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

  /**
   * Processes given input value which uses `this` resource and state.
   * `apply` automatically handles exceptions so that the resource is guaranteed to be released.
   *
   * @param value input value
   * */
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

  /**
   * Perform side effects after resource initialization, but before processing any input values.
   * The side effect is executed '''only if''' the resource was acquired successfully.
   *
   * @param f side effect to execute
   * @return `this` side effect
   * */
  def beforeAll(f: R => Unit): this.type = {
    self.beforeAlls += f
    self
  }

  /**
   * Perform side effects before resource released.
   * The side effect is executed '''only if''' all input values was processed without exceptions
   *
   * @param f side effect to execute
   * @return `this` side effect
   * */
  def afterAll(f: (R, S) => Unit): this.type = {
    afterAlls += f
    self
  }

  /**
   * Perform side effects after the resource released.
   * The side effect is always executed
   *
   * @param f side effect to execute
   * @return `this` side effect
   * */
  def onExit(f: => Unit): this.type = {
    self.onExits += (() => f)
    self
  }
}

object SideEffect {
  private val noopAcquire: () => Unit         = () => ()
  private val noopRelease: (Any, Any) => Unit = (_, _) => ()

  /** Side effect with resource, but without a state */
  type Stateless[R, A] = SideEffect[R, Unit, A]

  /** Side effect with state, but without a resource */
  type Resourceless[S, A] = SideEffect[Unit, S, A]

  /**
   * Builds a stateful side effect with a resource.
   *
   * @tparam R the resource type
   * @tparam S the state type
   * @tparam A side effect input type
   * @param initialState the initial state
   * @param acquire acquires the resource
   * @param release resource finalizer
   * @param use proceeds input value producing the updated state
   * @return a stateful side effect with resource
   * */
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

  /**
   * Builds a stateful side effect without a resource.
   *
   * @tparam S the state type
   * @tparam A side effect input type
   * @param initialState the initial state
   * @param use proceeds input value producing the updated state
   * @return a stateful side effect without resource
   * */
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

  /**
   * Builds a side effect with resource.
   *
   * @tparam R the resource type
   * @tparam A side effect input type
   * @param acquire acquires the resource
   * @param release resource finalizer
   * @param use proceeds input value producing the updated state
   * @return a side effect with resource
   * */
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
