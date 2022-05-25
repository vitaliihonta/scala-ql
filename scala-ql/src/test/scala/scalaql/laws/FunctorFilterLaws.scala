package scalaql.laws

import scalaql._
import org.scalacheck.Prop._

class FunctorFilterLaws extends ScalaqlLawsSpec("FunctorFilter") {
  property("mapFilter composition") = forAll {
    (testInput: QueryTestFixture, testData: TestData, f: StringMapFilter, g: StringMapFilter) =>
      import testData._

      val left = testInput.query.mapFilter(f.predicate).mapFilter(g.predicate).toList.run(from(strings) & from(ints))

      val right =
        testInput.query.mapFilter(a => f.predicate(a).flatMap(g.predicate)).toList.run(from(strings) & from(ints))

      val expected = testInput.plainFunc(strings, ints).flatMap(f.predicate(_)).flatMap(g.predicate(_))

      left == expected && left == right
  }

  property("mapFilter map consistency") = forAll {
    (testInput: QueryTestFixture, testData: TestData, f: StringTransformation) =>
      import testData._

      val left = testInput.query.mapFilter(f.func.andThen(Some(_))).toList.run(from(strings) & from(ints))

      val right = testInput.query.map(f.func).toList.run(from(strings) & from(ints))

      val expected = testInput.plainFunc(strings, ints).map(f.func)

      left == expected && left == right
  }

  property("collect consistent with mapFilter") = forAll {
    (testInput: QueryTestFixture, testData: TestData, p: StringMapFilter) =>
      import testData._

      val left = testInput.query.collect(p.partial).toList.run(from(strings) & from(ints))

      val right = testInput.query.mapFilter(p.partial.lift).toList.run(from(strings) & from(ints))

      val expected = testInput.plainFunc(strings, ints).collect(p.partial)

      left == expected && left == right
  }

  property("filter consistent with mapFilter") = forAll {
    (testInput: QueryTestFixture, testData: TestData, p: StringPredicate) =>
      import testData._

      val left = testInput.query.filter(p.predicate).toList.run(from(strings) & from(ints))

      val right = testInput.query.mapFilter(x => Some(x).filter(p.predicate)).toList.run(from(strings) & from(ints))

      val expected = testInput.plainFunc(strings, ints).filter(p.predicate)

      left == expected && left == right
  }

  property("filterNot consistent with filter") = forAll {
    (testInput: QueryTestFixture, testData: TestData, p: StringPredicate) =>
      import testData._

      val left = testInput.query.filterNot(p.predicate).toList.run(from(strings) & from(ints))

      val right = testInput.query.filter(!p.predicate(_)).toList.run(from(strings) & from(ints))

      val expected = testInput.plainFunc(strings, ints).filterNot(p.predicate)

      left == expected && left == right
  }
}
