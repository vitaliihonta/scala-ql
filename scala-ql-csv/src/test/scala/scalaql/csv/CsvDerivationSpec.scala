package scalaql.csv

import scalaql.ScalaqlUnitSpec

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

      implicitly[CsvDecoder.Row[Person]].readRow(
        CsvEntry.Row(
          Map(
            "id"                     -> "2769a48d-8fec-4242-81d1-959ae424712c",
            "name"                   -> "Vitalii",
            "workingExperienceYears" -> "100500",
            "birthDay"               -> "1997-11-13",
            "createdAt"              -> "2022-06-15T12:55",
            "isProgrammer"           -> "true"
          )
        )
      ) shouldEqual Person(
        id = UUID.fromString("2769a48d-8fec-4242-81d1-959ae424712c"),
        name = "Vitalii",
        workingExperienceYears = 100500,
        birthDay = LocalDate.of(1997, 11, 13),
        createdAt = LocalDateTime.of(2022, 6, 15, 12, 55, 0),
        isProgrammer = true
      )
    }

    "provide correct encoders" in {
      implicitly[CsvEncoder.Row[Person]].write(
        Person(
          id = UUID.fromString("2769a48d-8fec-4242-81d1-959ae424712c"),
          name = "Vitalii",
          workingExperienceYears = 100500,
          birthDay = LocalDate.of(1997, 11, 13),
          createdAt = LocalDateTime.of(2022, 6, 15, 12, 55, 0),
          isProgrammer = true
        )
      ) shouldEqual CsvEntry.Row(
        Map(
          "id"                     -> "2769a48d-8fec-4242-81d1-959ae424712c",
          "name"                   -> "Vitalii",
          "workingExperienceYears" -> "100500",
          "birthDay"               -> "1997-11-13",
          "createdAt"              -> "2022-06-15T12:55",
          "isProgrammer"           -> "true"
        )
      )
    }
  }
}
