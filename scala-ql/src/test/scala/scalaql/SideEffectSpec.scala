package scalaql

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

class SideEffectSpec extends ScalaqlUnitSpec {
  "SideEffect" should {
    "process and update state" in {
      val released = new AtomicBoolean(false)
      val counter  = new AtomicInteger()
      val sideEffect = SideEffect[AtomicBoolean, Int, String](
        initialState = 0,
        acquire = () => released,
        release = (ab, s) => {
          counter.addAndGet(s)
          ab.set(true)
        },
        use = (_, c, v) => c + v.length
      )

      val data = arbitraryN[String]
      data.foreach(sideEffect)
      sideEffect.close()

      assert(released.get())
      counter.get() shouldBe data.map(_.length).sum
    }

    "capture exceptions" in {
      val released = new AtomicBoolean(false)
      val sideEffect = SideEffect[AtomicBoolean, Unit, String](
        initialState = (),
        acquire = () => released,
        release = (ab, _) => ab.set(true),
        use = (_, _, _) => sys.error("BOOM!")
      )

      val data = arbitraryN[String]
      val error = intercept[RuntimeException] {
        try data.foreach(sideEffect)
        finally sideEffect.close()
      }

      assert(released.get())
      error.getMessage shouldBe "BOOM!"
    }

    "do onExit" in {
      val released = new AtomicBoolean(false)
      val exited   = new AtomicBoolean(false)
      val sideEffect = SideEffect[AtomicBoolean, Unit, String](
        initialState = (),
        acquire = () => released,
        release = (ab, _) => ab.set(true),
        use = (_, _, _) => sys.error("BOOM!")
      ).onExit(exited.set(true))

      val data = arbitraryN[String]
      val error = intercept[RuntimeException] {
        try data.foreach(sideEffect)
        finally sideEffect.close()
      }

      assert(released.get())
      assert(exited.get())
      error.getMessage shouldBe "BOOM!"
    }

    "do afterAll if no exceptions captured" in {
      val released       = new AtomicBoolean(false)
      val afterAllResult = new AtomicInteger()
      val counter        = new AtomicInteger()
      val sideEffect = SideEffect[AtomicBoolean, Int, String](
        initialState = 0,
        acquire = () => released,
        release = (ab, s) => {
          counter.addAndGet(s)
          ab.set(true)
        },
        use = (_, c, v) => c + v.length
      ).afterAll((_, s) => afterAllResult.set(s))

      val data = arbitraryN[String]
      data.foreach(sideEffect)
      sideEffect.close()

      assert(released.get())
      counter.get() shouldBe data.map(_.length).sum
      assert(afterAllResult.get() == counter.get())
    }

    "not do afterAll if exception captured" in {
      val released       = new AtomicBoolean(false)
      val afterAllCalled = new AtomicBoolean(false)
      val sideEffect = SideEffect[AtomicBoolean, Unit, String](
        initialState = (),
        acquire = () => released,
        release = (ab, _) => ab.set(true),
        use = (_, _, _) => sys.error("BOOM!")
      ).afterAll((_, _) => afterAllCalled.set(true))

      val data = arbitraryN[String]
      val error = intercept[RuntimeException] {
        try data.foreach(sideEffect)
        finally sideEffect.close()
      }

      assert(released.get())
      assert(!afterAllCalled.get())
      error.getMessage shouldBe "BOOM!"
    }

    "do beforeAll" in {
      val released        = new AtomicBoolean(false)
      val beforeAllCalled = new AtomicBoolean(false)
      val sideEffect = SideEffect[AtomicBoolean, Unit, String](
        initialState = (),
        acquire = () => released,
        release = (ab, _) => ab.set(true),
        use = (_, _, _) => sys.error("BOOM!")
      ).beforeAll(_ => beforeAllCalled.set(true))

      val data = arbitraryN[String]
      val error = intercept[RuntimeException] {
        try data.foreach(sideEffect)
        finally sideEffect.close()
      }

      assert(released.get())
      assert(beforeAllCalled.get())
      error.getMessage shouldBe "BOOM!"
    }

    "capture exceptions in beforeAll" in {
      val released    = new AtomicBoolean(false)
      val invocations = new AtomicInteger()
      val sideEffect = SideEffect[AtomicBoolean, Unit, String](
        initialState = (),
        acquire = () => released,
        release = (ab, _) => ab.set(true),
        use = (_, _, _) => invocations.incrementAndGet()
      ).beforeAll { _ =>
        sys.error("BOOOOM!")
      }

      val data = arbitraryN[String]
      val error = intercept[RuntimeException] {
        try data.foreach(sideEffect)
        finally sideEffect.close()
      }

      assert(released.get())
      assert(invocations.get() == 0)
      error.getMessage shouldBe "BOOOOM!"
    }
  }
}
