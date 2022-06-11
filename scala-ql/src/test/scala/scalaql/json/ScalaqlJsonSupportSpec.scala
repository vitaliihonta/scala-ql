package scalaql.json

import scalaql.*
import scala.util.Try
import io.circe.Decoder

class ScalaqlJsonSupportSpec extends ScalaqlUnitSpec {
  "ScalaqlJsonSupport" should {
    "correctly process simple csv" in {
      case class Person(name: String, age: Int)
      implicit val personDecoder: Decoder[Person] = Decoder.instance[Person] { c =>
        for {
          name <- c.get[String]("name")
          age  <- c.get[Int]("age")
        } yield Person(name, age)
      }

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
  }
}
