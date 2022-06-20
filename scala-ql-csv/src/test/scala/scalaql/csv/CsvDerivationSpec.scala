package scalaql.csv

import scalaql.ScalaqlUnitSpec
import scalaql.sources.Naming

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class CsvDerivationSpec extends ScalaqlUnitSpec {
  case class Person(
    id:                     UUID,
    name:                   String,
    workingExperienceYears: Int,
    birthDay:               LocalDate,
    createdAt:              LocalDateTime,
    isProgrammer:           Boolean)

  "CsvDerivation" should {
    "provide correct decoders" in {
      implicit val context: CsvContext = CsvContext(Nil, Naming.Literal)

      CsvDecoder[Person]
        .read(
          Map(
            "id"                     -> "2769a48d-8fec-4242-81d1-959ae424712c",
            "name"                   -> "Vitalii",
            "workingExperienceYears" -> "100500",
            "birthDay"               -> "1997-11-13",
            "createdAt"              -> "2022-06-15T12:55",
            "isProgrammer"           -> "true"
          )
        ) shouldEqual Right(
        Person(
          id = UUID.fromString("2769a48d-8fec-4242-81d1-959ae424712c"),
          name = "Vitalii",
          workingExperienceYears = 100500,
          birthDay = LocalDate.of(1997, 11, 13),
          createdAt = LocalDateTime.of(2022, 6, 15, 12, 55, 0),
          isProgrammer = true
        )
      )
    }

    "provide correct encoders" in {
      implicit val context: CsvContext = CsvContext(Nil, Naming.Literal)

      CsvEncoder[Person]
        .write(
          Person(
            id = UUID.fromString("2769a48d-8fec-4242-81d1-959ae424712c"),
            name = "Vitalii",
            workingExperienceYears = 100500,
            birthDay = LocalDate.of(1997, 11, 13),
            createdAt = LocalDateTime.of(2022, 6, 15, 12, 55, 0),
            isProgrammer = true
          )
        ) shouldEqual Map(
        "id"                     -> "2769a48d-8fec-4242-81d1-959ae424712c",
        "name"                   -> "Vitalii",
        "workingExperienceYears" -> "100500",
        "birthDay"               -> "1997-11-13",
        "createdAt"              -> "2022-06-15T12:55",
        "isProgrammer"           -> "true"
      )
    }
  }
}
