package scalaql.laws

import scalaql.*
import org.scalacheck.Prop.*

class MonadLaws extends ScalaqlLawsSpec("Monad") {
  property("flatMap associativity") = forAll {
    (
      testInput: QueryTestFixture,
      testData:  TestData,
      f:         StringBindTransformation,
      g:         StringBindTransformation
    ) =>
      import testData.*

      val left  = testInput.query.flatMap(f.bind).flatMap(g.bind).toList.run(from(strings) & from(ints))
      val right = testInput.query.flatMap(a => f.bind(a).flatMap(g.bind)).toList.run(from(strings) & from(ints))

      val expected =
        testInput.plainFunc(strings, ints).flatMap(f.asFlatMap(strings, _)).flatMap(g.asFlatMap(strings, _))

      left == expected && left == right
  }

  property("monad left identity") = forAll {
    (
      testData: TestData,
      f:        StringBindTransformation
    ) =>
      import testData.*

      val left = select.from(strings).flatMap(f.bind).toList.run(from(strings) & from(ints))

      val expected = strings.flatMap(f.asFlatMap(strings, _))

      left == expected
  }

  property("monad right identity") = forAll {
    (
      testInput: QueryTestFixture,
      testData:  TestData
    ) =>
      import testData.*

      val left  = testInput.query.flatMap(s => select.from(Seq(s))).toList.run(from(strings) & from(ints))
      val right = testInput.query.toList.run(from(strings) & from(ints))

      val expected = testInput.plainFunc(strings, ints)

      left == expected && left == right
  }

  property("map flatMap coherence") = forAll {
    (
      testInput: QueryTestFixture,
      testData:  TestData,
      f:         StringTransformation
    ) =>
      import testData.*

      val left  = testInput.query.flatMap(s => select.from(Seq(f.func(s)))).toList.run(from(strings) & from(ints))
      val right = testInput.query.map(f.func).toList.run(from(strings) & from(ints))

      val expected = testInput.plainFunc(strings, ints).map(f.func)

      left == expected && left == right
  }
}
