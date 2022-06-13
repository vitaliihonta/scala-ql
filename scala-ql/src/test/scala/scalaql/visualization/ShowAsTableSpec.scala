package scalaql.visualization

import scalaql.*

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

object ShowAsTableSpec {
  case class Person(name: String, age: Int)
  case class PersonWithHobbies(name: String, age: Int, hobbies: List[String])

  case class FavoriteBook(name: String, `type`: String)
  case class PersonWithFavoriteBook(name: String, age: Int, favoriteBook: FavoriteBook)

  case class PersonWithAttributes(name: String, age: Int, attributes: Map[String, String])

  case class Metadata(key: String, value: String)
  case class PersonWithMetadata(name: String, age: Int, metadata: Map[String, List[Metadata]])

}

class ShowAsTableSpec extends ScalaqlUnitSpec {
  import scalaql.visualization.ShowAsTableSpec.*

  "show" should {
    "correctly render simple table" in {
      val result = captureConsoleOut {
        select[Person]
          .show(truncate = false)
          .run(
            from(
              List(
                Person("Vitalii", 24),
                Person("Alice", 30),
                Person("Bob", 21)
              )
            )
          )
      }

      result shouldBe
        """\+-------+---+
           \|name   |age|
           \+-------+---+
           \|Vitalii|24 |
           \|Alice  |30 |
           \|Bob    |21 |
           \+-------+---+
           \
           \""".stripMargin('\\')
    }

    "correctly render list columns" in {
      val result = captureConsoleOut {
        select[PersonWithHobbies]
          .show(truncate = false)
          .run(
            from(
              List(
                PersonWithHobbies("Vitalii", 24, List("bicycle", "football")),
                PersonWithHobbies("Alice", 30, List("crypto")),
                PersonWithHobbies("Bob", 21, List("crypto"))
              )
            )
          )
      }

      result shouldBe
        """+-------+---+-------------------+
          \|name   |age|hobbies            |
          \+-------+---+-------------------+
          \|Vitalii|24 |[bicycle, football]|
          \|Alice  |30 |[crypto]           |
          \|Bob    |21 |[crypto]           |
          \+-------+---+-------------------+
          \
          \""".stripMargin('\\')
    }

    "correctly render nested fields" in {
      val result = captureConsoleOut {
        select[PersonWithFavoriteBook]
          .show(truncate = false)
          .run(
            from(
              List(
                PersonWithFavoriteBook("Vitalii", 24, FavoriteBook("Thinking, Fast and Slow", "non-fiction")),
                PersonWithFavoriteBook("Alice", 30, FavoriteBook("Scala for impatient", "programming")),
                PersonWithFavoriteBook("Bob", 21, FavoriteBook("Clean code", "programming"))
              )
            )
          )
      }

      result shouldBe
        """+-------+---+-----------------------+-----------------+
          \|name   |age|favoriteBook.name      |favoriteBook.type|
          \+-------+---+-----------------------+-----------------+
          \|Vitalii|24 |Thinking, Fast and Slow|non-fiction      |
          \|Alice  |30 |Scala for impatient    |programming      |
          \|Bob    |21 |Clean code             |programming      |
          \+-------+---+-----------------------+-----------------+
          \
          \""".stripMargin('\\')
    }

    "correctly show map" in {
      val result = captureConsoleOut {
        select[PersonWithAttributes]
          .show(truncate = false)
          .run(
            from(
              List(
                PersonWithAttributes("Vitalii", 24, Map("lang" -> "scala", "lang2" -> "python")),
                PersonWithAttributes("Alice", 30, Map("colleague" -> "Bob")),
                PersonWithAttributes("Bob", 21, Map("favorite_cypher" -> "RSA"))
              )
            )
          )
      }

      result shouldBe
        """+-------+---+----------------------------+
          \|name   |age|attributes                  |
          \+-------+---+----------------------------+
          \|Vitalii|24 |{lang: scala, lang2: python}|
          \|Alice  |30 |{colleague: Bob}            |
          \|Bob    |21 |{favorite_cypher: RSA}      |
          \+-------+---+----------------------------+
          \
          \""".stripMargin('\\')
    }

    "correctly show nested" in {
      val result = captureConsoleOut {
        select[PersonWithMetadata]
          .show(truncate = false)
          .run(
            from(
              List(
                PersonWithMetadata(
                  name = "Vitalii",
                  age = 24,
                  metadata = Map(
                    "langs" -> List(
                      Metadata("functional", "scala"),
                      Metadata("dynamic", "python")
                    )
                  )
                ),
                PersonWithMetadata(
                  name = "Alice",
                  age = 30,
                  metadata = Map(
                    "colleagues" -> List(
                      Metadata("best", "Bob")
                    )
                  )
                ),
                PersonWithMetadata(
                  name = "Bob",
                  age = 21,
                  metadata = Map(
                    "cyphers" -> List(
                      Metadata("favorite", "RSA"),
                      Metadata("worst", "Cesar")
                    )
                  )
                )
              )
            )
          )
      }

      result shouldBe
        """+-------+---+-------------------------------------------------------------------------+
          \|name   |age|metadata                                                                 |
          \+-------+---+-------------------------------------------------------------------------+
          \|Vitalii|24 |{langs: [{key: functional, value: scala}, {key: dynamic, value: python}]}|
          \|Alice  |30 |{colleagues: [{key: best, value: Bob}]}                                  |
          \|Bob    |21 |{cyphers: [{key: favorite, value: RSA}, {key: worst, value: Cesar}]}     |
          \+-------+---+-------------------------------------------------------------------------+
          \
          \""".stripMargin('\\')
    }
  }

  private def captureConsoleOut[U](thunk: => U): String = {
    val baos        = new ByteArrayOutputStream
    val printStream = new PrintStream(baos)
    Console.withOut(printStream)(thunk)
    new String(baos.toByteArray, StandardCharsets.UTF_8)
  }
}
