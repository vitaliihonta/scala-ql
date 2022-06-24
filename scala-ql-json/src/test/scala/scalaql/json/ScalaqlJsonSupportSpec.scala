package scalaql.json

import scalaql.*
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.syntax._
import java.nio.file.Files
import java.nio.file.Path
import scala.collection.mutable

class ScalaqlJsonSupportSpec extends ScalaqlUnitSpec {
  case class Person(name: String, age: Int)
  implicit val personDecoder: Decoder[Person] = Decoder.instance[Person] { c =>
    for {
      name <- c.get[String]("name")
      age  <- c.get[Int]("age")
    } yield Person(name, age)
  }

  implicit val personEncoder: Encoder[Person] = Encoder.instance[Person] { p =>
    Json.obj("name" -> p.name.asJson, "age" -> p.age.asJson)
  }

  "ScalaqlJsonSupport" should {
    "correctly process simple json" in {

      val rawJson =
        """|{"name": "vitalii", "age": 24}
           |{"name": "john", "age": 100}""".stripMargin

      select[Person]
        .where(_.age == 24)
        .toList
        .run(
          from(
            json.read.string(rawJson)
          )
        ) should contain theSameElementsAs {
        List(Person(name = "vitalii", age = 24))
      }
    }

    "correctly read single line json" in {
      implicit val config: JsonReadConfig = JsonReadConfig.default.copy(multiline = false)

      val actualResult = select[Person].toList
        .run(
          from(
            json.read.string[Person](
              """[
                |{
                |   "name" : "vitalii",
                |   "age" : 24
                |},
                |{
                |   "name" : "john",
                |   "age" : 100
                |}
                |]""".stripMargin
            )
          )
        )

      val expectedResult = List(Person(name = "vitalii", age = 24), Person(name = "john", age = 100))

      assert(actualResult == expectedResult)
    }

    "correctly read from files" in {
      val path = Files.createTempFile("scala-ql-json", "read-spec")
      writeIntoFile(
        path,
        """|{"name": "vitalii", "age": 24}
           |{"name": "john", "age": 100}""".stripMargin
      )

      select[Person].toList
        .run(
          from(
            json.read.file[Person](path)
          )
        ) should contain theSameElementsAs {
        List(Person(name = "vitalii", age = 24), Person(name = "john", age = 100))
      }
    }
    "correctly read from multiple files" in {
      val files = for (i <- 1 to 10) yield {
        val file = Files.createTempFile("scala-ql-json", "read-files-spec")
        writeIntoFile(
          file,
          s"""|{"name": "vitalii", "age": $i}
              |{"name": "john", "age": $i}""".stripMargin
        )
        file
      }

      val actualResult = select[Person].toList
        .run(
          from(
            json.read.files[Person]()(
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

    "correctly read glob from directory" in {
      val dir = Files.createTempDirectory("scala-ql-json")
      for (i <- 1 to 10) {
        val file = Files.createTempFile(dir, "scala-ql-json", "read-glob-spec")
        writeIntoFile(
          file,
          s"""|{"name": "vitalii", "age": $i}
              |{"name": "john", "age": $i}""".stripMargin
        )
      }

      val actualResult = select[Person].toList
        .run(
          from(
            json.read.directory[Person](dir, globPattern = "*")
          )
        )

      val expectedResult = (1 to 10).flatMap { i =>
        List(Person(name = "vitalii", age = i), Person(name = "john", age = i))
      }.toList

      actualResult should contain theSameElementsAs {
        expectedResult
      }
    }

    "correctly write simple json" in {
      val sb = new mutable.StringBuilder
      select[Person]
        .foreach(
          json.write.string[Person](sb)
        )
        .run(
          from(
            List(Person(name = "vitalii", age = 24), Person(name = "john", age = 100))
          )
        )

      val expectedResult =
        """|{"name":"vitalii","age":24}
           |{"name":"john","age":100}
           |""".stripMargin.replace("\n", "\r\n")

      val actualResult = sb.toString

      assert(actualResult == expectedResult)
    }

    "correctly write single line json" in {
      implicit val config: JsonWriteConfig = JsonWriteConfig.default.copy(multiline = false)

      val sb = new mutable.StringBuilder
      select[Person]
        .foreach(
          json.write.string[Person](sb)
        )
        .run(
          from(
            List(Person(name = "vitalii", age = 24), Person(name = "john", age = 100))
          )
        )

      val expectedResult =
        """[
          |{
          |   "name" : "vitalii",
          |   "age" : 24
          |},
          |{
          |   "name" : "john",
          |   "age" : 100
          |}
          |]""".stripMargin.replace("\n", "\r\n")

      val actualResult = sb.toString

      assert(actualResult == expectedResult)
    }

    "correctly write into files" in {
      val path = Files.createTempFile("scala-json-csv", "write-spec")

      select[Person]
        .foreach(
          json.write.file[Person](path)
        )
        .run(
          from(
            List(Person(name = "vitalii", age = 24), Person(name = "john", age = 100))
          )
        )

      val expectedResult =
        """|{"name":"vitalii","age":24}
           |{"name":"john","age":100}
           |""".stripMargin.replace("\n", "\r\n")

      assert(readFromFile(path) == expectedResult)
    }
  }

  private def readFromFile(path: Path): String =
    new String(Files.readAllBytes(path))

  private def writeIntoFile(path: Path, content: String): Unit =
    Files.write(path, content.getBytes)
}
