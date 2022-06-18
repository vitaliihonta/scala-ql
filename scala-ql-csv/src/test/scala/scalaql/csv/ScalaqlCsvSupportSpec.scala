package scalaql.csv

import scalaql.ScalaqlUnitSpec
import scalaql.*

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import scala.collection.mutable
import scala.util.Try

class ScalaqlCsvSupportSpec extends ScalaqlUnitSpec {
  case class Person(name: String, age: Int)

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
            csv.read.string[Person](rawCsv)
          )
        ) should contain theSameElementsAs {
        List(Person(name = "vitalii", age = 24))
      }
    }

    "correctly read from files" in {
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
            csv.read.file[Person](path)
          )
        ) should contain theSameElementsAs {
        List(Person(name = "vitalii", age = 24), Person(name = "john", age = 100))
      }
    }

    "correctly read glob from file" in {
      val dir = Files.createTempDirectory("scala-ql-csv")
      for (i <- 1 to 10) {
        val file = Files.createTempFile(dir, "scala-ql-csv", "read-glob-spec")
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
            csv.read.directory[Person](dir, globPattern = "*")
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
          csv.write.string[Person](sb)
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
          csv.write.file[Person](path)
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
  }

  private def readFromFile(path: Path): String =
    new String(Files.readAllBytes(path))

  private def writeIntoFile(path: Path, content: String): Unit =
    Files.write(path, content.getBytes)
}
