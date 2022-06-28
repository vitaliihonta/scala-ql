package scalaql.csv

import scalaql.ScalaqlUnitSpec
import scalaql.*
import scalaql.sources.Naming

import java.nio.file.Files
import java.nio.file.Path
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID
import scala.collection.mutable

class ScalaqlCsvSupportSpec extends ScalaqlUnitSpec {
  case class Person(name: String, age: Int)

  case class PersonInfo(name: String, workingExperienceYears: Int, birthDay: LocalDate, isProgrammer: Boolean)
  case class Metadata(id: UUID, createdAt: LocalDateTime)
  case class NestedPersonOption(info: PersonInfo, metadata: Option[Metadata])

  "ScalaqlCsvSupport" should {
    "correctly read simple csv" in {
      val rawCsv =
        """|name,age
           |vitalii,24
           |john,100""".stripMargin

      select[Person]
        .where(_.age == 24)
        .toList
        .run(
          from(
            csv.read[Person].string(rawCsv)
          )
        ) should contain theSameElementsAs {
        List(Person(name = "vitalii", age = 24))
      }
    }

    "correctly read nested csv with options" in {

      val actualResult = select[NestedPersonOption].toList
        .run(
          from(
            csv
              .read[NestedPersonOption]
              .option(Naming.SnakeCase)
              .string(
                """name,working_experience_years,birth_day,is_programmer,id,created_at
                  |Vitalii,100500,1997-11-13,true,2769a48d-8fec-4242-81d1-959ae424712c,2022-06-15T12:55
                  |John,2000,1922-07-16,true,,
                  |""".stripMargin
              )
          )
        )

      val expectedResult = List(
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
        ),
        NestedPersonOption(
          metadata = None,
          info = PersonInfo(
            name = "John",
            workingExperienceYears = 2000,
            birthDay = LocalDate.of(1922, 7, 16),
            isProgrammer = true
          )
        )
      )

      assert(actualResult == expectedResult)
    }

    "correctly read from multiple file" in {
      val files = for (i <- 1 to 10) yield {
        val file = Files.createTempFile("scala-ql-csv", "read-files-spec")
        writeIntoFile(
          file,
          s"""|name,age
              |vitalii,$i
              |john,$i""".stripMargin
        )
        file
      }

      val actualResult = select[Person].toList
        .run(
          from(
            csv
              .read[Person]
              .files()(
                files: _*
              )
          )
        )

      val expectedResult = (1 to 10).flatMap { i =>
        List(Person(name = "vitalii", age = i), Person(name = "john", age = i))
      }.toList

      actualResult should contain theSameElementsAs {
        expectedResult
      }
    }

    "correctly read from directory" in {
      val path = Files.createTempFile("scala-ql-csv", "read-spec")
      writeIntoFile(
        path,
        """|name,age
           |vitalii,24
           |john,100""".stripMargin
      )

      select[Person].toList
        .run(
          from(
            csv.read[Person].file(path)
          )
        ) should contain theSameElementsAs {
        List(Person(name = "vitalii", age = 24), Person(name = "john", age = 100))
      }
    }

    "correctly read glob from directory" in {
      val dir = Files.createTempDirectory("scala-ql-csv")
      for (i <- 1 to 10) {
        val file = Files.createTempFile(dir, "scala-ql-csv", "read-glob-spec.csv")
        writeIntoFile(
          file,
          s"""|name,age
              |vitalii,$i
              |john,$i""".stripMargin
        )
      }

      val actualResult = select[Person].toList
        .run(
          from(
            csv.read[Person].directory(dir, globPattern = "**.csv")
          )
        )

      val expectedResult = (1 to 10).flatMap { i =>
        List(Person(name = "vitalii", age = i), Person(name = "john", age = i))
      }.toList

      actualResult should contain theSameElementsAs {
        expectedResult
      }
    }

    "correctly write simple csv" in {
      val sb = new mutable.StringBuilder
      select[Person]
        .foreach(
          csv.write[Person].string(sb)
        )
        .run(
          from(
            List(Person(name = "vitalii", age = 24), Person(name = "john", age = 100))
          )
        )

      val expectedResult =
        """name,age
          |vitalii,24
          |john,100
          |""".stripMargin.replace("\n", "\r\n")

      val actualResult = sb.toString

      assert(actualResult == expectedResult)
    }

    "correctly write into files" in {
      val path = Files.createTempFile("scala-ql-csv", "write-spec")

      select[Person]
        .foreach(
          csv.write[Person].file(path)
        )
        .run(
          from(
            List(Person(name = "vitalii", age = 24), Person(name = "john", age = 100))
          )
        )

      val expectedResult =
        """name,age
          |vitalii,24
          |john,100
          |""".stripMargin.replace("\n", "\r\n")

      assert(readFromFile(path) == expectedResult)
    }

    "correctly write nested csv with options" in {
      val sb = new mutable.StringBuilder
      select[NestedPersonOption]
        .foreach(
          csv
            .write[NestedPersonOption]
            .option(Naming.SnakeCase)
            .string(sb)
        )
        .run(
          from(
            List(
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
              ),
              NestedPersonOption(
                metadata = None,
                info = PersonInfo(
                  name = "John",
                  workingExperienceYears = 2000,
                  birthDay = LocalDate.of(1922, 7, 16),
                  isProgrammer = true
                )
              )
            )
          )
        )

      val expectedResult =
        """name,working_experience_years,birth_day,is_programmer,id,created_at
          |Vitalii,100500,1997-11-13,true,2769a48d-8fec-4242-81d1-959ae424712c,2022-06-15T12:55
          |John,2000,1922-07-16,true,,
          |""".stripMargin.replace("\n", "\r\n")

      val actualResult = sb.toString

      assert(actualResult == expectedResult)
    }
  }

  private def readFromFile(path: Path): String =
    new String(Files.readAllBytes(path))

  private def writeIntoFile(path: Path, content: String): Unit =
    Files.write(path, content.getBytes)
}
