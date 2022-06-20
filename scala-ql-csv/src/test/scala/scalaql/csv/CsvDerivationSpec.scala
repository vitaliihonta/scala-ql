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

  case class PersonInfo(name: String, workingExperienceYears: Int, birthDay: LocalDate, isProgrammer: Boolean)
  case class Metadata(id: UUID, createdAt: LocalDateTime)
  case class NestedPerson(metadata: Metadata, info: PersonInfo)

  "CsvDecoder" should {
    "decode correctly with default config" in {
      implicit val context: CsvContext = CsvContext.initial(Naming.Literal)

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

    "decode correctly with naming" in {
      implicit val context: CsvContext = CsvContext.initial(Naming.SnakeCase)

      CsvDecoder[Person]
        .read(
          Map(
            "id"                       -> "2769a48d-8fec-4242-81d1-959ae424712c",
            "name"                     -> "Vitalii",
            "working_experience_years" -> "100500",
            "birth_day"                -> "1997-11-13",
            "created_at"               -> "2022-06-15T12:55",
            "is_programmer"            -> "true"
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

    "decode correctly for nested fields" in {
      implicit val context: CsvContext = CsvContext.initial(Naming.Literal)

      CsvDecoder[NestedPerson]
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
        NestedPerson(
          metadata = Metadata(
            id = UUID.fromString("2769a48d-8fec-4242-81d1-959ae424712c"),
            createdAt = LocalDateTime.of(2022, 6, 15, 12, 55, 0)
          ),
          info = PersonInfo(
            name = "Vitalii",
            workingExperienceYears = 100500,
            birthDay = LocalDate.of(1997, 11, 13),
            isProgrammer = true
          )
        )
      )
    }

    "decode handling errors" in {
      implicit val context: CsvContext = CsvContext.initial(Naming.Literal)

      val result = CsvDecoder[Person]
        .read(
          Map(
            "id"                     -> "xxx",
            "name"                   -> "Vitalii",
            "workingExperienceYears" -> "yyy",
            "birthDay"               -> "zzz",
            "createdAt"              -> "2022-06-15T12:55",
            "isProgrammer"           -> "true"
          )
        )

      assert(result.isLeft)
      val error = result.swap.getOrElse(???)
      error shouldBe a[CsvDecoderAccumulatingException]
      error.toString shouldBe
        """scalaql.csv.CsvDecoderAccumulatingException: Failed to decode Person (at root): 
          |	+ ( scalaql.csv.CsvDecoderException: Cannot decode cell at path `id`: java.lang.IllegalArgumentException: Invalid UUID string: xxx )
          |	+ ( scalaql.csv.CsvDecoderException: Cannot decode cell at path `workingExperienceYears`: java.lang.NumberFormatException: For input string: "yyy" )
          |	+ ( scalaql.csv.CsvDecoderException: Cannot decode cell at path `birthDay`: java.time.format.DateTimeParseException: Text 'zzz' could not be parsed at index 0 )
          |""".stripMargin
    }
  }

  "CsvEncoder" should {

    "encode correctly with default config" in {
      implicit val context: CsvContext = CsvContext.initial(Naming.Literal)

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

    "encode correctly with naming" in {
      implicit val context: CsvContext = CsvContext.initial(Naming.SnakeCase)

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
        "id"                       -> "2769a48d-8fec-4242-81d1-959ae424712c",
        "name"                     -> "Vitalii",
        "working_experience_years" -> "100500",
        "birth_day"                -> "1997-11-13",
        "created_at"               -> "2022-06-15T12:55",
        "is_programmer"            -> "true"
      )
    }

    "encode correctly for nested fields" in {
      implicit val context: CsvContext = CsvContext.initial(Naming.Literal)

      CsvEncoder[NestedPerson]
        .write(
          NestedPerson(
            metadata = Metadata(
              id = UUID.fromString("2769a48d-8fec-4242-81d1-959ae424712c"),
              createdAt = LocalDateTime.of(2022, 6, 15, 12, 55, 0)
            ),
            info = PersonInfo(
              name = "Vitalii",
              workingExperienceYears = 100500,
              birthDay = LocalDate.of(1997, 11, 13),
              isProgrammer = true
            )
          )
        ) shouldEqual
        Map(
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
