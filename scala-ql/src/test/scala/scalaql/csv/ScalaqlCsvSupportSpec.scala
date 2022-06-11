package scalaql.csv

import scalaql.ScalaqlUnitSpec

import scalaql.*
import scala.util.Try

class ScalaqlCsvSupportSpec extends ScalaqlUnitSpec {
  "ScalaqlCsvSupport" should {
    "correctly process simple csv" in {
      case class Person(name: String, age: Int)
      implicit val personCsvDecoder: CsvDecoder.Row[Person] = new CsvDecoder.Row[Person] {
        override def readRow(value: CsvDecoderInput.Row): Person = {
          val result = for {
            name   <- value.row.get("name")
            rawAge <- value.row.get("age")
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
            csv.string(rawCsv)
          )
        ) should contain theSameElementsAs {
        List(Person(name = "vitalii", age = 24))
      }
    }
  }
}
