package scalaql.csv

import scalaql.ScalaqlUnitSpec

import scalaql.*
import scala.util.Try

class ScalaqlCsvSupportSpec extends ScalaqlUnitSpec {
  "ScalaqlCsvSupport" should {
    "correctly process simple csv" in {
      case class Person(name: String, age: Int)
      implicit val personCsvDecoder: CsvDecoder[Person] = new CsvDecoder[Person] {
        override def read(value: Map[String, String]): Person = {
          val result = for {
            name   <- value.get("name")
            rawAge <- value.get("age")
            age    <- Try(rawAge.toInt).toOption
          } yield Person(name, age)
          result.getOrElse(throw new IllegalArgumentException(s"Invalid CSV: expected Person like, got $value"))
        }
      }

      val rawCsv =
        """|name,age
           |vitalii,24
           |john,100""".stripMargin

      select[Person]
        .where(_.age == 24)
        .toList
        .run(
          from(
            csv.fromString(rawCsv)
          )
        ) should contain theSameElementsAs {
        List(Person(name = "vitalii", age = 24))
      }
    }
  }
}
