package scalaql.laws

import org.scalacheck.Prop.forAll
import scalaql.*

class MonoidKLaws extends ScalaqlLawsSpec("MonoidK") {
  property("monoidK left identity") = forAll { (testInput: QueryTestFixture, testData: TestData) =>
    import testData.*

    val empty = select.from(Nil)

    val left     = (testInput.query ++ empty).toList.run(from(strings) & from(ints))
    val right    = testInput.query.toList.run(from(strings) & from(ints))
    val expected = testInput.plainFunc(strings, ints)

    left == expected && left == right
  }

  property("monoidK right identity") = forAll { (testInput: QueryTestFixture, testData: TestData) =>
    import testData.*

    val empty = select.from(Nil)

    val left     = (empty ++ testInput.query).toList.run(from(strings) & from(ints))
    val right    = testInput.query.toList.run(from(strings) & from(ints))
    val expected = testInput.plainFunc(strings, ints)

    left == expected && left == right
  }

  property("semigroupK associative") = forAll {
    (xs: QueryTestFixture, ys: QueryTestFixture, zs: QueryTestFixture, testData: TestData) =>
      import testData.*

      val left  = (xs.query ++ (ys.query ++ zs.query)).toList.run(from(strings) & from(ints))
      val right = ((xs.query ++ ys.query) ++ zs.query).toList.run(from(strings) & from(ints))

      val expected = xs.plainFunc(strings, ints) ++ ys.plainFunc(strings, ints) ++ zs.plainFunc(strings, ints)

      left == expected && left == right
  }
}
