package scalaql

import scalaql.fixture.*

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import scala.util.Random

class BaseLineSpec extends ScalaqlUnitSpec {
  case class PeopleStats(profession: Profession, avgAge: Double)

  "scalaql" should {
    "correctly process query without input" in repeated {
      val people = arbitraryN[Person]

      val query: Query[Any, String] =
        select
          .from(people)
          .collect { case Person(name, _, Profession.Unemployed, _) =>
            s"Unemployed $name"
          }
          .sorted

      val expectedResult = people.collect { case Person(name, _, Profession.Unemployed, _) =>
        s"Unemployed $name"
      }.sorted

      query.toList.run shouldEqual expectedResult
    }

    "correctly process query with const" in repeated {
      val companies = arbitraryN[Company]
      val offices   = arbitraryN[Office](Office.arbitrary(companies))

      def minFloors(company: Company): Int = company.industry match {
        case Industry.Fintech   => 5
        case Industry.Medtech   => 3
        case Industry.ECommerce => 10
      }

      val query = select[Company]
        .whereSubQuery(company => select.from(offices).exists(_.floors >= minFloors(company)))
        .map(_.name)

      val expectedResult = companies
        .filter(company => offices.exists(_.floors >= minFloors(company)))
        .map(_.name)

      query.toList.run(from(companies)) shouldEqual expectedResult
    }

    "correctly execute foreach" in repeated {
      val companies = arbitraryN[Company]

      var counter = 0
      val query = select[Company]
        .map(_.name.length)
        .foreach(counter += _)

      val expectedResult = companies
        .map(_.name.length)
        .sum

      query.run(from(companies))

      counter shouldEqual expectedResult
    }

    "correctly process foreach multiple times" in repeated {
      val people = arbitraryN[Person]

      val counter = new AtomicInteger()

      val released = new AtomicInteger()

      val capturedNames = mutable.ListBuffer.empty[String]

      val query = select[Person]
        .foreach(
          SideEffect[mutable.ListBuffer[String], AtomicInteger, Person](
            initialState = new AtomicInteger(),
            acquire = () => mutable.ListBuffer.empty,
            release = (buffer, c) => {
              released.incrementAndGet()
              capturedNames ++= buffer
              counter.addAndGet(c.get())
            },
            use = (r, s, a) => {
              r.append(a.name)
              s.incrementAndGet()
              s
            }
          )
        )

      query.run(from(people))

      counter.get() shouldBe people.size
      released.get() shouldBe 1
      capturedNames.toList shouldBe people.map(_.name)

      query.run(from(people))

      counter.get() shouldBe people.size * 2
      released.get() shouldBe 2
      capturedNames.toList shouldBe people.map(_.name) ++ people.map(_.name)
    }

    "correctly process simple map + filter + sort" in repeated {
      val people = arbitraryN[Person]

      val query: Query[From[Person], Person] = select[Person]
        .where(_.profession == Profession.Developer)
        .where(_.age >= 18)
        .sortBy(_.age)
        .map(person => person.copy(name = s"Engineer ${person.name}"))

      val expectedResult =
        people
          .filter(_.profession == Profession.Developer)
          .filter(_.age >= 18)
          .sortBy(_.age)
          .map(person => person.copy(name = s"Engineer ${person.name}"))

      query.toList.run(from(people)) shouldEqual expectedResult
    }

    "correctly process simple flatMap + map + filter" in repeated {
      val companies = arbitraryN[Company]
      val people    = arbitraryN[Person]

      val query: Query[From[Person] & From[Company], Employee] =
        for {
          person <- select[Person]
          if person.age >= 18
          company <- select[Company]
          if person.industry == company.industry
        } yield Employee(person, company.name)

      val expectedResult =
        people
          .filter(_.age >= 18)
          .flatMap { person =>
            companies
              .filter(_.industry == person.industry)
              .map(company => Employee(person, company.name))
          }

      query.toList.run(from(people) & from(companies)) should contain theSameElementsAs expectedResult
    }

    "correctly process multiple flatMap's" in repeated {
      val companies  = arbitraryN[Company]
      val offices    = arbitraryN[Office](Office.arbitrary(companies))
      val people     = arbitraryN[Person]
      val workspaces = arbitraryN[Workspace](Workspace.arbitrary(people, offices))

      val query: Query[From[Person] & From[Company] & From[Workspace] & From[Office], EmployeesInfo] =
        for {
          person <- select[Person]
          if person.age >= 18
          company <- select[Company]
          if person.industry == company.industry
          workspace <- select[Workspace]
          if workspace.employee == person.name
          office <- select[Office]
          if workspace.office == office.location
        } yield EmployeesInfo(person, company.name, office.location, workspace.floor)

      val expectedResult =
        people
          .filter(_.age >= 18)
          .flatMap { person =>
            companies
              .filter(_.industry == person.industry)
              .flatMap { company =>
                workspaces
                  .filter(_.employee == person.name)
                  .flatMap { workspace =>
                    offices
                      .filter(_.location == workspace.office)
                      .map(office => EmployeesInfo(person, company.name, office.location, workspace.floor))
                  }
              }
          }

      query.toList.run(
        from(people) & from(companies) & from(workspaces) & from(offices)
      ) should contain theSameElementsAs expectedResult
    }

    "correctly process simple groupBy + aggregate" in repeated {
      val people = arbitraryN[Person]

      val query: Query[From[Person], PeopleStats] = select[Person]
        .groupBy(_.profession)
        .aggregate((profession, person) => person.avgBy(_.age.toDouble).map(PeopleStats(profession, _)))

      val expectedResult =
        people.groupBy(_.profession).map { case (profession, people) =>
          PeopleStats(profession, people.map(_.age).sum.toDouble / people.size)
        }

      query.toList.run(from(people)) shouldEqual expectedResult
    }

    "correctly process multiple aggregations" in repeated {
      val people = arbitraryN[Person]

      val query: Query[From[Person], (Profession, Double, Set[Profession], Set[Industry])] = select[Person]
        .groupBy(_.profession)
        .aggregate { (profession, person) =>
          person.const(profession) &&
          person.avgBy(_.age.toDouble) &&
          person.distinctBy(_.profession) &&
          person.flatDistinctBy(_.profession.industries)
        }

      val expectedResult =
        people.groupBy(_.profession).map { case (profession, people) =>
          (
            profession,
            people.map(_.age.toDouble).sum / people.size,
            people.map(_.profession).toSet,
            people.flatMap(_.profession.industries).toSet
          )
        }

      query.toList.run(from(people)) shouldEqual expectedResult
    }

    "correctly process groupBy with multiple columns" in repeated {
      val people = arbitraryN[Person]

      val query: Query[From[Person], (Profession, Int, Set[Profession], Double, Int)] = select[Person]
        .groupBy(_.profession, _.age)
        .aggregate { case ((profession, age), person) =>
          person.const(profession) &&
          person.const(age) &&
          person.distinctBy(_.profession) &&
          person.avgBy(_.age.toDouble) &&
          person.sumBy(_.age)
        }

      val expectedResult =
        people.groupBy(p => (p.profession, p.age)).map { case ((profession, age), people) =>
          (
            profession,
            age,
            people.map(_.profession).toSet,
            people.map(_.age.toDouble).sum / people.size,
            people.map(_.age).sum
          )
        }

      query.toList.run(from(people)) shouldEqual expectedResult
    }

    "correctly process simple filterM + map + filter" in repeated {
      val companies = arbitraryN[Company]
      val people    = arbitraryN[Person]

      val query: Query[From[Company] & From[Person], Profession] =
        select[Person]
          .whereSubQuery(person => select[Company].exists(_.industry == person.industry))
          .where(_.age >= 30)
          .map(_.profession)

      val expectedResult =
        people
          .filter(_.age >= 30)
          .flatMap { person =>
            if (companies.exists(_.industry == person.industry)) Some(person.profession)
            else None
          }

      val actual = query.toList.run(from(people) & from(companies))

      actual should contain theSameElementsAs expectedResult
    }

    "correctly process self-flatMap" in repeated {
      val people = arbitraryN[Person]

      val query = for {
        p1 <- select[Person]
        if p1.age >= 18
        p2 <- select[Person]
        if p1.profession == p2.profession
      } yield (p1, p2)

      val expectedResult = for {
        p1 <- people
        if p1.age >= 18
        p2 <- people
        if p1.profession == p2.profession
      } yield (p1, p2)

      query.toList.run(from(people)) should contain theSameElementsAs expectedResult
    }

    "correctly process inner join" in {
      val people    = arbitraryN[Person]
      val companies = arbitraryN[Company]

      val query = select[Person]
        .where(_.age >= 18)
        .join(select[Company])
        .on(_.profession.industries contains _.industry)

      val expectedResult =
        people
          .filter(_.age >= 18)
          .flatMap { person =>
            companies
              .filter(company => person.profession.industries contains company.industry)
              .map(company => (person, company))
          }

      query.toList.run(from(people) & from(companies)) should contain theSameElementsAs expectedResult
    }

    "correctly process cross join" in repeated {
      val people    = arbitraryN[Person]
      val companies = arbitraryN[Company]

      val query = select[Person]
        .where(_.age >= 18)
        .crossJoin(select[Company])
        .on(_.profession.industries contains _.industry)

      val expectedResult =
        people
          .filter(_.age >= 18)
          .flatMap { person =>
            companies
              .filter(company => person.profession.industries contains company.industry)
              .map(company => (person, company))
          }

      query.toList.run(from(people) & from(companies)) should contain theSameElementsAs expectedResult
    }

    "correctly process left join" in repeated {
      val people    = arbitraryN[Person]
      val companies = arbitraryN[Company]

      val query = select[Person]
        .where(_.age >= 18)
        .leftJoin(select[Company])
        .on(_.profession.industries contains _.industry)

      val expectedResult =
        people
          .filter(_.age >= 18)
          .flatMap { person =>
            val joined = companies.filter(company => person.profession.industries contains company.industry)
            if (joined.isEmpty) List(person -> None)
            else joined.map(c => person -> Some(c))
          }

      query.toList.run(from(people) & from(companies)) should contain theSameElementsAs expectedResult
    }

    "correctly process self-join" in {
      val people = arbitraryN[Person]

      val query = select[Person]
        .join(select[Person])
        .on(_.profession == _.profession)

      val expectedResult =
        people
          .flatMap { p1 =>
            people
              .filter(p2 => p1.profession == p2.profession)
              .map(p2 => (p1, p2))
          }

      query.toList.run(from(people)) should contain theSameElementsAs expectedResult
    }

    "correctly process query with simple union" in repeated {
      val people = arbitraryN[Person]

      val query = select[Person].where(_.profession == Profession.Developer) ++
        select[Person].where(_.age >= 18)

      val expectedResult =
        people.filter(_.profession == Profession.Developer) ++ people.filter(_.age >= 18)

      query.toList.run(from(people)) should contain theSameElementsAs expectedResult
    }

    "correctly process query with complex union" in repeated {
      val companies  = arbitraryN[Company]
      val offices    = arbitraryN[Office](Office.arbitrary(companies))
      val people     = arbitraryN[Person]
      val workspaces = arbitraryN[Workspace](Workspace.arbitrary(people, offices))

      def result(p: Person, w: Workspace) = s"${p.name} is on ${w.floor} floor of ${w.office}"

      val query = (select[Person].where(_.profession == Profession.Developer) ++
        select[Person].where(_.age >= 18) ++
        select[Person].whereSubQuery(person =>
          select[Company].exists(c => person.profession.industries contains c.industry)
        ))
        .join(select[Workspace])
        .on(_.name == _.employee)
        .map { case (p, w) => result(p, w) }

      val expectedResult = {
        val peopleUnion = people.filter(_.profession == Profession.Developer) ++ people.filter(_.age >= 18) ++
          people.filter(person => companies.exists(c => person.profession.industries contains c.industry))

        peopleUnion.flatMap { p =>
          workspaces
            .filter(_.employee == p.name)
            .map(result(p, _))
        }
      }

      query.toList.run(
        from(workspaces) & from(companies) & from(people)
      ) should contain theSameElementsAs expectedResult
    }

    "correctly process query with andThen" in repeated {
      case class WorkspaceInfo(workspace: Workspace, office: Office)

      val companies  = arbitraryN[Company]
      val offices    = arbitraryN[Office](Office.arbitrary(companies))
      val people     = arbitraryN[Person]
      val workspaces = arbitraryN[Workspace](Workspace.arbitrary(people, offices))

      val query1 = select[Workspace]
        .join(select[Office])
        .on(_.office == _.location)
        .map((WorkspaceInfo.apply _).tupled)

      val query2 = select[WorkspaceInfo]
        .where(_.office.floors > 2)
        .map(_.office.company)

      val query: Query[From[Office] & From[Workspace], String] = query1 >>> query2

      val expectedResult: Set[String] =
        workspaces
          .flatMap { workspace =>
            offices
              .filter(_.location == workspace.office)
              .filter(_.floors > 2)
              .map(_.company)
          }
          .distinct
          .toSet

      query.distinct.run(from(offices) & from(workspaces)) shouldEqual expectedResult
    }

    "correctly process query with deduplicateBy" in {
      val people    = arbitraryN[Person]
      val companies = arbitraryN[Company]

      val query = select[Person]
        .deduplicateBy(_.name)
        .where(_.age >= 18)
        .join(select[Company].deduplicateBy(_.name))
        .on(_.profession.industries contains _.industry)

      val expectedResult =
        people
          .filter(_.age >= 18)
          .flatMap { person =>
            companies
              .filter(company => person.profession.industries contains company.industry)
              .map(company => (person, company))
          }

      val duplicatedPeople    = Random.shuffle(people ++ people ++ people)
      val duplicatedCompanies = Random.shuffle(companies ++ companies ++ companies)

      query.toList.run(
        from(duplicatedPeople) & from(duplicatedCompanies)
      ) should contain theSameElementsAs expectedResult
    }

    "correctly process query with statefulMapConcat" in {
      val people = arbitraryN[Person]

      val query = select[Person]
        .statefulMapConcat(initialState = 1) { (n, person) =>
          n + 1 -> List.fill(n)(person)
        }

      val expectedResult =
        people.zipWithIndex.flatMap { case (person, n) =>
          List.fill(n + 1)(person)
        }

      query.toList.run(from(people)) should contain theSameElementsAs expectedResult
    }
  }
}
