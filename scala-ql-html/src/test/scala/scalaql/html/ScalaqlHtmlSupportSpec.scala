package scalaql.html

import scalaql.*
import scalatags.Text
import scalatags.Text.all.{html => _, select => _, *}
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import scala.collection.mutable

class ScalaqlHtmlSupportSpec extends ScalaqlUnitSpec {
  case class Person(
    id:                     UUID,
    name:                   String,
    workingExperienceYears: Int,
    birthDay:               LocalDate,
    createdAt:              LocalDateTime,
    isProgrammer:           Boolean)

  case class PeopleStats(
    isProgrammer: Boolean,
    stats:        List[PeopleStatsPerIsProgrammer])

  case class PeopleStatsPerIsProgrammer(
    birthYear:            Int,
    avgWorkingExperience: Double,
    records:              List[PersonRecord])

  case class PersonRecord(
    id:                     UUID,
    name:                   String,
    workingExperienceYears: Int,
    birthDay:               LocalDate,
    createdAt:              LocalDateTime)

  "ScalaqlHtmlSupport" should {
    "write simple html correctly" in {
      val resultBuilder = new mutable.StringBuilder

      select[Person]
        .foreach(
          html.write.string[Person](resultBuilder)
        )
        .run(
          from(
            List(
              Person(
                id = UUID.fromString("4ffe9631-2169-4c50-90ff-8818bc28ab3f"),
                name = "Vitalii",
                workingExperienceYears = 100500,
                birthDay = LocalDate.of(1997, 11, 13),
                createdAt = LocalDateTime.of(2022, 6, 19, 15, 0),
                isProgrammer = true
              ),
              Person(
                id = UUID.fromString("304e27cc-f2e2-489a-8fac-4279abcbbefa"),
                name = "John",
                workingExperienceYears = 2000,
                birthDay = LocalDate.of(1922, 6, 19),
                createdAt = LocalDateTime.of(2022, 6, 19, 15, 0),
                isProgrammer = true
              )
            )
          )
        )

      println(resultBuilder)

      resultBuilder.toString shouldEqual
        """<html><head></head><body><table><tr><th>id</th><th>name</th><th>workingExperienceYears</th><th>birthDay</th><th>createdAt</th><th>isProgrammer</th></tr><tr><td>4ffe9631-2169-4c50-90ff-8818bc28ab3f</td><td>Vitalii</td><td>100500</td><td>1997-11-13</td><td>2022-06-19T15:00</td><td>true</td></tr><tr><td>304e27cc-f2e2-489a-8fac-4279abcbbefa</td><td>John</td><td>2000</td><td>1922-06-19</td><td>2022-06-19T15:00</td><td>true</td></tr></table></body></html>"""
    }

    "write nested html correctly" in {
      val resultBuilder = new mutable.StringBuilder

      select[Person]
        .groupBy(_.isProgrammer)
        .aggregate { (_, people) =>
          people.report(_.birthDay.getYear) { (bdayYear, people) =>
            people.const(bdayYear) &&
            people.avgBy(_.workingExperienceYears.toDouble) &&
            people.toList
          }
        }
        .map { case (isProgrammer, stats) =>
          PeopleStats(
            isProgrammer,
            stats.map { case (birthYear, avgWorkingExperienceYears, people) =>
              PeopleStatsPerIsProgrammer(
                birthYear,
                avgWorkingExperienceYears,
                people.map { person =>
                  PersonRecord(
                    id = person.id,
                    name = person.name,
                    workingExperienceYears = person.workingExperienceYears,
                    birthDay = person.birthDay,
                    createdAt = person.createdAt
                  )
                }
              )
            }
          )
        }
        .foreach(
          html.write.string[PeopleStats](resultBuilder)
        )
        .run(
          from(
            List(
              Person(
                id = UUID.fromString("4ffe9631-2169-4c50-90ff-8818bc28ab3f"),
                name = "Vitalii",
                workingExperienceYears = 100500,
                birthDay = LocalDate.of(1997, 11, 13),
                createdAt = LocalDateTime.of(2022, 6, 19, 15, 0),
                isProgrammer = true
              ),
              Person(
                id = UUID.fromString("304e27cc-f2e2-489a-8fac-4279abcbbefa"),
                name = "John",
                workingExperienceYears = 2000,
                birthDay = LocalDate.of(1922, 6, 19),
                createdAt = LocalDateTime.of(2022, 6, 19, 15, 0),
                isProgrammer = true
              )
            )
          )
        )

      println(resultBuilder)

      // TODO: groupBy in scala 2.12 doesn't preserve order?
      val expectedResults = List(
        """<html><head></head><body><table><tr><th>isProgrammer</th><th>birthYear</th><th>avgWorkingExperience</th><th>id</th><th>name</th><th>workingExperienceYears</th><th>birthDay</th><th>createdAt</th></tr><tr><td>true</td><td></td><tr><td></td><td>1922</td><td>2000.0</td><tr><td></td><td></td><td></td><td>304e27cc-f2e2-489a-8fac-4279abcbbefa</td><td>John</td><td>2000</td><td>1922-06-19</td><td>2022-06-19T15:00</td></tr><td></td><td></td><td></td><td></td><td></td></tr><tr><td></td><td>1997</td><td>100500.0</td><tr><td></td><td></td><td></td><td>4ffe9631-2169-4c50-90ff-8818bc28ab3f</td><td>Vitalii</td><td>100500</td><td>1997-11-13</td><td>2022-06-19T15:00</td></tr><td></td><td></td><td></td><td></td><td></td></tr><td></td><td></td><td></td><td></td><td></td><td></td></tr></table></body></html>""",
        """<html><head></head><body><table><tr><th>isProgrammer</th><th>birthYear</th><th>avgWorkingExperience</th><th>id</th><th>name</th><th>workingExperienceYears</th><th>birthDay</th><th>createdAt</th></tr><tr><td>true</td><td></td><tr><td></td><td>1997</td><td>100500.0</td><tr><td></td><td></td><td></td><td>4ffe9631-2169-4c50-90ff-8818bc28ab3f</td><td>Vitalii</td><td>100500</td><td>1997-11-13</td><td>2022-06-19T15:00</td></tr><td></td><td></td><td></td><td></td><td></td></tr><tr><td></td><td>1922</td><td>2000.0</td><tr><td></td><td></td><td></td><td>304e27cc-f2e2-489a-8fac-4279abcbbefa</td><td>John</td><td>2000</td><td>1922-06-19</td><td>2022-06-19T15:00</td></tr><td></td><td></td><td></td><td></td><td></td></tr><td></td><td></td><td></td><td></td><td></td><td></td></tr></table></body></html>"""
      )

      expectedResults should contain(resultBuilder.toString)
    }

    "write html with styles" in {
      val resultBuilder = new mutable.StringBuilder

      implicit val styling: HtmlStyling[PeopleStats] = new HtmlStyling[PeopleStats] {
        private val headers = Map
          .empty[String, List[Text.Modifier]]
          .withDefaultValue(
            List(color.red, border := "3px solid")
          )

        private val fields = Map(
          "isProgrammer"           -> List(color.yellow),
          "birthYear"              -> List(color.blue),
          "workingExperienceYears" -> List(color.blue),
          "id"                     -> List(),
          "name"                   -> List(),
          "workingExperienceYears" -> List(),
          "birthDay"               -> List(),
          "createdAt"              -> List()
        ).withDefaultValue(List())

        override def headerStyle(name: String): List[Text.Modifier] = headers(name)

        override def fieldStyle(name: String): List[Text.Modifier] = fields(name)
      }

      implicit val config: HtmlTableEncoderConfig[PeopleStats] =
        HtmlTableEncoderConfig.default.copy(
          tableTag = table(tableLayout.fixed, borderCollapse.collapse, width := "100%"),
          bodyTag = body(),
          rowTag = tr(borderBottom := "2px solid #ddd")
        )

      select[Person]
        .groupBy(_.isProgrammer)
        .aggregate { (_, people) =>
          people.report(_.birthDay.getYear) { (bdayYear, people) =>
            people.const(bdayYear) &&
            people.avgBy(_.workingExperienceYears.toDouble) &&
            people.toList
          }
        }
        .map { case (isProgrammer, stats) =>
          PeopleStats(
            isProgrammer,
            stats.map { case (birthYear, avgWorkingExperienceYears, people) =>
              PeopleStatsPerIsProgrammer(
                birthYear,
                avgWorkingExperienceYears,
                people.map { person =>
                  PersonRecord(
                    id = person.id,
                    name = person.name,
                    workingExperienceYears = person.workingExperienceYears,
                    birthDay = person.birthDay,
                    createdAt = person.createdAt
                  )
                }
              )
            }
          )
        }
        .foreach(
          html.write.string[PeopleStats](resultBuilder)
        )
        .run(
          from(
            List(
              Person(
                id = UUID.fromString("4ffe9631-2169-4c50-90ff-8818bc28ab3f"),
                name = "Vitalii",
                workingExperienceYears = 100500,
                birthDay = LocalDate.of(1997, 11, 13),
                createdAt = LocalDateTime.of(2022, 6, 19, 15, 0),
                isProgrammer = true
              ),
              Person(
                id = UUID.fromString("304e27cc-f2e2-489a-8fac-4279abcbbefa"),
                name = "John",
                workingExperienceYears = 2000,
                birthDay = LocalDate.of(1922, 6, 19),
                createdAt = LocalDateTime.of(2022, 6, 19, 15, 0),
                isProgrammer = true
              )
            )
          )
        )

      println(resultBuilder)
    }

  }
}
