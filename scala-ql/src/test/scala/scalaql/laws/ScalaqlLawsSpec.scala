package scalaql.laws

import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Properties
import scalaql._

abstract class ScalaqlLawsSpec(name: String) extends Properties(name) {

  case class QueryTestFixture(
    query:     Query[From[String] with From[Int], String],
    plainFunc: (List[String], List[Int]) => List[String])

  case class TestData(strings: List[String], ints: List[Int])

  protected implicit val arbitraryTestData: Arbitrary[TestData] = Arbitrary(
    for {
      strings <- Gen.listOfN(n = 20, Arbitrary.arbitrary[String])
      ints    <- Gen.listOfN(n = 20, Arbitrary.arbitrary[Int])
    } yield TestData(strings, ints)
  )

  case class StringTransformation(func: String => String)

  protected implicit val arbitraryStringTransformation: Arbitrary[StringTransformation] = {
    Arbitrary(
      Gen.oneOf(
        List(
          StringTransformation(_.toUpperCase),
          StringTransformation(_.take(6)),
          StringTransformation(_ + "-fooooo"),
          StringTransformation(_.replaceAll("a", "bar")),
          StringTransformation(_.trim.toLowerCase)
        )
      )
    )
  }

  case class StringPredicate(predicate: String => Boolean)

  protected implicit val arbitraryStringPredicate: Arbitrary[StringPredicate] = {
    Arbitrary(
      Gen.oneOf(
        List(
          StringPredicate(_.length > 5),
          StringPredicate(_.isEmpty),
          StringPredicate(s => s.nonEmpty && s.head.isUpper),
          StringPredicate(s => s.reverse == s)
        )
      )
    )
  }

  case class StringMapFilter(partial: PartialFunction[String, String]) {
    val predicate: String => Option[String] = partial.lift
  }

  protected implicit val arbitraryStringMapFilter: Arbitrary[StringMapFilter] = {
    Arbitrary(
      Gen.oneOf(
        List(
          StringMapFilter({ case s if s.nonEmpty => s }),
          StringMapFilter({ case s if s.nonEmpty => s.head.toString + s.toUpperCase }),
          StringMapFilter({ case s if s.length % 2 == 0 => s.trim }),
          StringMapFilter({ case s if s.reverse == s => s.toLowerCase })
        )
      )
    )
  }

  case class StringBindTransformation(
    bind:      String => Query[From[String] with From[Int], String],
    asFlatMap: (List[String], String) => List[String])

  protected implicit val arbitraryStringBindTransformation: Arbitrary[StringBindTransformation] = {
    Arbitrary(
      Gen.oneOf(
        List(
          StringBindTransformation(
            s => select.from((1 to s.length / 10).map(_.toString)),
            (_, s) => (1 to s.length / 10).map(_.toString).toList
          ),
          StringBindTransformation(
            s => select[String].filter(_.length == s.length).map(_.toUpperCase),
            (strings, s) => strings.filter(_.length == s.length).map(_.toUpperCase)
          ),
          StringBindTransformation(
            s =>
              for {
                s1 <- select[String]
                if s1.length < s.length
              } yield s + s1,
            (strings, s) =>
              for {
                s1 <- strings
                if s1.length < s.length
              } yield s + s1
          )
        )
      )
    )
  }

  protected implicit val arbitraryTestInput: Arbitrary[QueryTestFixture] = {
    val simple: QueryTestFixture = QueryTestFixture(select[String], (strings, _) => strings)

    val filtered: QueryTestFixture =
      QueryTestFixture(select[String].filter(_.length > 5), (strings, _) => strings.filter(_.length > 5))

    val mapped: QueryTestFixture =
      QueryTestFixture(select[String].map(_.toUpperCase), (strings, _) => strings.map(_.toUpperCase))

    val chained: QueryTestFixture = QueryTestFixture(
      select[String].filter(_.length > 2).map(_.capitalize).sorted,
      (strings, _) => strings.filter(_.length > 2).map(_.capitalize).sorted
    )

    val bind: QueryTestFixture = {
      val query = for {
        string <- select[String]
        int    <- select[Int]
        if int > 0
      } yield s"$string-$int"

      val plainFunc = (strings: List[String], ints: List[Int]) =>
        for {
          string <- strings
          int    <- ints
          if int > 0
        } yield s"$string-$int"

      QueryTestFixture(query, plainFunc)
    }

    Arbitrary(
      Gen.oneOf(
        List(
          simple,
          filtered,
          mapped,
          chained,
          bind
        )
      )
    )
  }
}
