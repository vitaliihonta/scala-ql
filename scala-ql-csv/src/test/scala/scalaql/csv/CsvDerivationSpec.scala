package scalaql.csv

import scalaql.{Naming, ScalaqlUnitSpec}

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

  case class PersonWithOption(
    id:         UUID,
    name:       String,
    profession: Option[String])

  case class PersonInfo(name: String, workingExperienceYears: Int, birthDay: LocalDate, isProgrammer: Boolean)
  case class Metadata(id: UUID, createdAt: LocalDateTime)
  case class NestedPerson(metadata: Metadata, info: PersonInfo)

  case class NestedPersonOption(info: PersonInfo, metadata: Option[Metadata])

  "CsvDecoder" should {
    "decode correctly with default config" in {
      implicit val context: CsvReadContext =
        CsvReadContext.initial(Naming.Literal, caseSensitive = true, emptyStringInOptions = true)

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
      implicit val context: CsvReadContext =
        CsvReadContext.initial(Naming.SnakeCase, caseSensitive = true, emptyStringInOptions = true)

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
      implicit val context: CsvReadContext =
        CsvReadContext.initial(Naming.Literal, caseSensitive = true, emptyStringInOptions = true)

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

    "decode options correctly" in {
      implicit val context: CsvReadContext =
        CsvReadContext.initial(Naming.Literal, caseSensitive = true, emptyStringInOptions = true)
      CsvDecoder[PersonWithOption]
        .read(
          Map(
            "id"   -> "2769a48d-8fec-4242-81d1-959ae424712c",
            "name" -> "Vitalii"
          )
        ) shouldEqual Right(
        PersonWithOption(
          id = UUID.fromString("2769a48d-8fec-4242-81d1-959ae424712c"),
          name = "Vitalii",
          profession = None
        )
      )
    }

    "decode nested options correctly" in {
      implicit val context: CsvReadContext =
        CsvReadContext.initial(Naming.Literal, caseSensitive = true, emptyStringInOptions = true)
      CsvDecoder[NestedPersonOption]
        .read(
          Map(
            "name"                   -> "Vitalii",
            "workingExperienceYears" -> "100500",
            "birthDay"               -> "1997-11-13",
            "isProgrammer"           -> "true"
          )
        ) shouldEqual Right(
        NestedPersonOption(
          metadata = None,
          info = PersonInfo(
            name = "Vitalii",
            workingExperienceYears = 100500,
            birthDay = LocalDate.of(1997, 11, 13),
            isProgrammer = true
          )
        )
      )

      CsvDecoder[NestedPersonOption]
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
        NestedPersonOption(
          metadata = Some(
            Metadata(
              id = UUID.fromString("2769a48d-8fec-4242-81d1-959ae424712c"),
              createdAt = LocalDateTime.of(2022, 6, 15, 12, 55, 0)
            )
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
      implicit val context: CsvReadContext =
        CsvReadContext.initial(Naming.Literal, caseSensitive = true, emptyStringInOptions = true)

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
      error shouldBe a[CsvDecoderException.Accumulating]
      error.toString shouldBe
        """scalaql.csv.CsvDecoderException$Accumulating: Failed to decode Person (at `root`):
          |	+ ( scalaql.csv.CsvDecoderException$CannotDecode: Cannot decode cell at path `id`: java.lang.IllegalArgumentException: Invalid UUID string: xxx )
          |	+ ( scalaql.csv.CsvDecoderException$CannotDecode: Cannot decode cell at path `workingExperienceYears`: java.lang.NumberFormatException: For input string: "yyy" )
          |	+ ( scalaql.csv.CsvDecoderException$CannotDecode: Cannot decode cell at path `birthDay`: java.time.format.DateTimeParseException: Text 'zzz' could not be parsed at index 0 )
          |""".stripMargin
    }

    "decode nested options handling errors" in {
      implicit val context: CsvReadContext =
        CsvReadContext.initial(Naming.Literal, caseSensitive = true, emptyStringInOptions = true)

      val result = CsvDecoder[NestedPersonOption]
        .read(
          Map(
            "id"                     -> "yyy",
            "name"                   -> "Vitalii",
            "workingExperienceYears" -> "100500",
            "birthDay"               -> "zzz",
            "createdAt"              -> "xxx",
            "isProgrammer"           -> "true"
          )
        )

      assert(result.isLeft)
      val error = result.swap.getOrElse(???)
      error shouldBe a[CsvDecoderException.Accumulating]
      error.toString shouldBe
        """scalaql.csv.CsvDecoderException$Accumulating: Failed to decode NestedPersonOption (at `root`):
          |	+ ( scalaql.csv.CsvDecoderException$CannotDecode: Cannot decode cell at path `info.birthDay`: java.time.format.DateTimeParseException: Text 'zzz' could not be parsed at index 0 )
          |	+ ( scalaql.csv.CsvDecoderException$CannotDecode: Cannot decode cell at path `metadata.id`: java.lang.IllegalArgumentException: Invalid UUID string: yyy )
          |	+ ( scalaql.csv.CsvDecoderException$CannotDecode: Cannot decode cell at path `metadata.createdAt`: java.time.format.DateTimeParseException: Text 'xxx' could not be parsed at index 0 )
          |""".stripMargin
    }
  }

  "CsvEncoder" should {

    "encode correctly with default config" in {
      implicit val context: CsvWriteContext = CsvWriteContext.initial(headers = Nil, Naming.Literal)

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
      implicit val context: CsvWriteContext = CsvWriteContext.initial(headers = Nil, Naming.SnakeCase)

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
      implicit val context: CsvWriteContext = CsvWriteContext.initial(headers = Nil, Naming.Literal)

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

    "encode options correctly" in {
      implicit val context: CsvWriteContext = CsvWriteContext.initial(headers = Nil, Naming.Literal)
      CsvEncoder[PersonWithOption]
        .write(
          PersonWithOption(
            id = UUID.fromString("2769a48d-8fec-4242-81d1-959ae424712c"),
            name = "Vitalii",
            profession = None
          )
        ) shouldEqual
        Map(
          "id"   -> "2769a48d-8fec-4242-81d1-959ae424712c",
          "name" -> "Vitalii"
        )
    }

    "encode nested options correctly" in {
      implicit val context: CsvWriteContext = CsvWriteContext.initial(headers = Nil, Naming.Literal)
      CsvEncoder[NestedPersonOption]
        .write(
          NestedPersonOption(
            metadata = None,
            info = PersonInfo(
              name = "Vitalii",
              workingExperienceYears = 100500,
              birthDay = LocalDate.of(1997, 11, 13),
              isProgrammer = true
            )
          )
        ) shouldEqual
        Map(
          "name"                   -> "Vitalii",
          "workingExperienceYears" -> "100500",
          "birthDay"               -> "1997-11-13",
          "isProgrammer"           -> "true"
        )

      CsvEncoder[NestedPersonOption]
        .write(
          NestedPersonOption(
            metadata = Some(
              Metadata(
                id = UUID.fromString("2769a48d-8fec-4242-81d1-959ae424712c"),
                createdAt = LocalDateTime.of(2022, 6, 15, 12, 55, 0)
              )
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
