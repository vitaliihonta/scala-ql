package scalaql.csv

import scalaql.ScalaqlUnitSpec

class CsvDerivationSpec extends ScalaqlUnitSpec {
  "CsvDerivation" should {
    "provide correct decoders" in {
      case class Person(name: String, age: Int)

      summon[CsvDecoder.Row[Person]].readRow(
        CsvEntry.Row(
          Map("name" -> "vitalii", "age" -> "24")
        )
      ) shouldEqual Person(name = "vitalii", age = 24)
    }
  }
}
