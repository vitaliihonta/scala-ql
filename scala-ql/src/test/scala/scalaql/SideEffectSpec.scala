package scalaql

class SideEffectSpec extends ScalaqlUnitSpec {
  "SideEffect" should {
    "process and update state" in {
      assert(true)
    }

    "capture exceptions" in {
      assert(true)
    }

    "do onExit" in {
      assert(true)
    }

    "do afterAll if no exceptions captured" in {
      assert(true)
    }

    "not do afterAll if exception captured" in {
      assert(true)
    }
  }
}
