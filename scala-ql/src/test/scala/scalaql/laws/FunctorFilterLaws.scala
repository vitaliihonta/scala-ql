package scalaql.laws

import scalaql.*
import org.scalacheck.Prop.*
import scalaql.internal.PartialFunctionAndThenCompat

class FunctorFilterLaws extends ScalaqlLawsSpec("FunctorFilter") {
  property("where composition") = forAll {
    (testInput: QueryTestFixture, testData: TestData, f: StringPredicate, g: StringPredicate) =>
      import testData.*

      val left = testInput.query.where(f.predicate).where(g.predicate).toList.run(from(strings) & from(ints))

      val right =
        testInput.query.where(a => f.predicate(a) && g.predicate(a)).toList.run(from(strings) & from(ints))

      val expected = testInput.plainFunc(strings, ints).filter(f.predicate(_)).filter(g.predicate(_))

      left == expected && left == right
  }

  property("collect composition") = forAll {
    (testInput: QueryTestFixture, testData: TestData, f: StringCollect, g: StringCollect) =>
      import testData.*

      val left = testInput.query.collect(f.partial).collect(g.partial).toList.run(from(strings) & from(ints))

      val right =
        testInput.query
          .collect(PartialFunctionAndThenCompat.andThen(f.partial, g.partial))
          .toList
          .run(from(strings) & from(ints))

      val expected = testInput.plainFunc(strings, ints).collect(f.partial).collect(g.partial)

      left == expected && left == right
  }

  property("filter consistent with collect") = forAll {
    (testInput: QueryTestFixture, testData: TestData, p: StringPredicate) =>
      import testData.*

      val left = testInput.query.where(p.predicate).toList.run(from(strings) & from(ints))

      val right = testInput.query.collect { case x if p.predicate(x) => x }.toList.run(from(strings) & from(ints))

      val expected = testInput.plainFunc(strings, ints).filter(p.predicate)

      left == expected && left == right
  }

  property("filterNot consistent with filter") = forAll {
    (testInput: QueryTestFixture, testData: TestData, p: StringPredicate) =>
      import testData.*

      val left = testInput.query.whereNot(p.predicate).toList.run(from(strings) & from(ints))

      val right = testInput.query.where(!p.predicate(_)).toList.run(from(strings) & from(ints))

      val expected = testInput.plainFunc(strings, ints).filterNot(p.predicate)

      left == expected && left == right
  }
}
