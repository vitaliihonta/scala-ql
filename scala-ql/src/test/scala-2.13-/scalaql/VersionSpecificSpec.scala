package scalaql

import scalaql.fixture.*

class VersionSpecificSpec extends ScalaqlUnitSpec {
  "scalaql" should {
    "correctly collect to vector" in repeated {
      val people = arbitraryN[Person]

      val query: Query[From[Person], Person] = select[Person]
        .where(_.profession == Profession.Developer)
        .where(_.age >= 18)
        .orderBy(_.age)
        .map(person => person.copy(name = s"Engineer ${person.name}"))

      val expectedResult = people
        .filter(_.profession == Profession.Developer)
        .filter(_.age >= 18)
        .sortBy(_.age)
        .map(person => person.copy(name = s"Engineer ${person.name}"))
        .toVector

      query.to[Vector].run(from(people)) shouldEqual expectedResult
    }

    "correctly process aliased queries" in {
      case class Peer(who: String, age: Int, peer: String)
      sealed trait left
      sealed trait right

      val people1 = arbitraryN[Person]
      val people2 = arbitraryN[Person]

      val peers =
        select[Person]
          .as[left]
          .join(select[Person].as[right])
          .on(_.age == _.age)
          .map { case (left, right) =>
            Peer(
              who = left.name,
              age = left.age,
              peer = right.name
            )
          }

      val actualResult = peers.toList.run(
        from(people1).as[left] &
          from(people2).as[right]
      )

      val expectedResult = people1.flatMap { p1 =>
        people2
          .filter(_.age == p1.age)
          .map(p2 => Peer(p1.name, p1.age, p2.name))
      }

      actualResult.sortBy(_.who) should contain theSameElementsAs {
        expectedResult.sortBy(_.who)
      }
    }
  }
}
