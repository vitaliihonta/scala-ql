package scalaql.fixture

import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import spire.algebra.Eq

sealed trait Industry extends Product with Serializable

object Industry {
  case object Fintech   extends Industry
  case object Medtech   extends Industry
  case object ECommerce extends Industry

  val values: List[Industry] = List(Fintech, Medtech, ECommerce)

  implicit val eq: Eq[Industry]               = Eq.fromUniversalEquals[Industry]
  implicit val arbitrary: Arbitrary[Industry] = Arbitrary(Gen.oneOf(values))
}

case class Company(name: String, industry: Industry)

object Company {

  implicit val arbitrary: Arbitrary[Company] = Arbitrary(
    for {
      name     <- Gen.alphaStr
      industry <- Arbitrary.arbitrary[Industry]
    } yield Company(name, industry)
  )
}

case class Office(location: String, company: String, floors: Int)

object Office {

  def arbitrary(companies: Seq[Company]): Arbitrary[Office] = Arbitrary(
    for {
      name    <- Gen.alphaStr
      company <- Gen.oneOf(companies)
      floors  <- Gen.chooseNum(1, 50)
    } yield Office(name, company.name, floors)
  )
}

case class Workspace(employee: String, office: String, floor: Int)

object Workspace {

  def arbitrary(people: Seq[Person], offices: Seq[Office]): Arbitrary[Workspace] = Arbitrary(
    for {
      person <- Gen.oneOf(people)
      office <- Gen.oneOf(offices)
      floor  <- Gen.choose(1, office.floors)
    } yield Workspace(person.name, office.location, floor)
  )
}

case class Employee(person: Person, company: String)

case class EmployeesInfo(person: Person, company: String, location: String, floor: Int)

sealed abstract class Profession(val industries: List[Industry]) extends Product with Serializable

object Profession {
  case object Developer            extends Profession(List(Industry.Fintech, Industry.Medtech, Industry.ECommerce))
  case object Manager              extends Profession(List(Industry.Fintech, Industry.Medtech, Industry.ECommerce))
  case object SalesManager         extends Profession(List(Industry.Medtech, Industry.ECommerce))
  case object FraudSecurityManager extends Profession(List(Industry.Fintech))
  case object Unemployed           extends Profession(Nil)

  val values: List[Profession] = List(Developer, Manager, Unemployed)

  implicit val eq: Eq[Profession]               = Eq.fromUniversalEquals[Profession]
  implicit val arbitrary: Arbitrary[Profession] = Arbitrary(Gen.oneOf(values))
}

case class Person(
  name:       String,
  age:        Int,
  profession: Profession,
  industry:   Industry,
  salary:     BigDecimal)

object Person {

  implicit val arbitrary: Arbitrary[Person] = Arbitrary(
    for {
      name       <- Gen.alphaStr
      age        <- Gen.chooseNum(1, 80)
      industry   <- Arbitrary.arbitrary[Industry]
      profession <- Gen.oneOf(Profession.values.filter(_.industries contains industry))
      salary     <- Gen.chooseNum(BigDecimal(50000), BigDecimal(100000))
    } yield Person(name, age, profession, industry, salary)
  )
}
