package scalaql

import scalaql.fixture._

class QueryToSyntaxSpec extends ScalaqlUnitSpec {
  "QueryToSyntax" should {
    "correctly collect to vector" in repeated {
      val people = arbitraryN[Person]

      val query: Query[From[Person], Person] = select[Person]
        .where(_.profession == Profession.Developer)
        .where(_.age >= 18)
        .sortBy(_.age)
        .map(person => person.copy(name = s"Engineer ${person.name}"))

      val expectedResult = people
        .filter(_.profession == Profession.Developer)
        .filter(_.age >= 18)
        .sortBy(_.age)
        .map(person => person.copy(name = s"Engineer ${person.name}"))
        .toVector

      query.to[Vector].run(from(people)) shouldEqual expectedResult
    }
  }
}
