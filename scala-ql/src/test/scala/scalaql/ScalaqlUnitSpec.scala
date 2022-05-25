package scalaql

import org.scalacheck.rng.Seed
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

abstract class ScalaqlUnitSpec extends AnyWordSpec with Matchers {

  protected def totalRepeats: Int = 20

  protected def repeated[U](thunk: => U): U = {
    var res = thunk
    for (_ <- 1 to 20) res = thunk
    res
  }

  protected def arbitrary[A: Arbitrary]: A = arbitrary(Arbitrary.arbitrary[A])

  protected def arbitrary[A](gen: Gen[A]): A =
    gen.pureApply(
      Gen.Parameters.default.withSize(1),
      Seed.random()
    )

  protected def arbitraryN[A: Arbitrary]: List[A] =
    arbitraryN(Arbitrary.arbitrary[A])

  protected def arbitraryN[A](gen: Gen[A]): List[A] =
    Arbitrary(Gen.nonEmptyListOf(gen)).arbitrary
      .pureApply(
        Gen.Parameters.default,
        Seed.random()
      )

}
