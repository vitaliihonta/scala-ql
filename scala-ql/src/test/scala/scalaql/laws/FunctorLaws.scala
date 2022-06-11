package scalaql.laws

import org.scalacheck.Prop.*
import scalaql.*

class FunctorLaws extends ScalaqlLawsSpec("Functor") {
  property("covariant identity") = forAll { (testInput: QueryTestFixture, testData: TestData) =>
    import testData.*

    testInput.query.map(identity).toList.run(from(strings) & from(ints)) == testInput.plainFunc(strings, ints)
  }

  property("covariant composition") = forAll {
    (
      testInput: QueryTestFixture,
      testData:  TestData,
      f:         StringTransformation,
      g:         StringTransformation
    ) =>
      import testData.*

      val left     = testInput.query.map(f.func).map(g.func).toList.run(from(strings) & from(ints))
      val right    = testInput.query.map(f.func.andThen(g.func)).toList.run(from(strings) & from(ints))
      val expected = testInput.plainFunc(strings, ints).map(f.func).map(g.func)

      left == expected && left == right
  }
}
