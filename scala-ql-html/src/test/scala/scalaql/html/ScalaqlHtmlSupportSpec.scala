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

      // TODO: groupBy in scala 2.12 doesn't preserve order?
      val expectedResults = List(
        """<html><head></head><body><table><tr><th>isProgrammer</th><th>birthYear</th><th>avgWorkingExperience</th><th>id</th><th>name</th><th>workingExperienceYears</th><th>birthDay</th><th>createdAt</th></tr><tr><td>true</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr><tr><td></td><td>1922</td><td>2000.0</td><td></td><td></td><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td><td>304e27cc-f2e2-489a-8fac-4279abcbbefa</td><td>John</td><td>2000</td><td>1922-06-19</td><td>2022-06-19T15:00</td></tr><tr><td></td><td>1997</td><td>100500.0</td><td></td><td></td><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td><td>4ffe9631-2169-4c50-90ff-8818bc28ab3f</td><td>Vitalii</td><td>100500</td><td>1997-11-13</td><td>2022-06-19T15:00</td></tr></table></body></html>""",
        """<html><head></head><body><table><tr><th>isProgrammer</th><th>birthYear</th><th>avgWorkingExperience</th><th>id</th><th>name</th><th>workingExperienceYears</th><th>birthDay</th><th>createdAt</th></tr><tr><td>true</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr><tr><td></td><td>1997</td><td>100500.0</td><td></td><td></td><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td><td>4ffe9631-2169-4c50-90ff-8818bc28ab3f</td><td>Vitalii</td><td>100500</td><td>1997-11-13</td><td>2022-06-19T15:00</td></tr><tr><td></td><td>1922</td><td>2000.0</td><td></td><td></td><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td><td>304e27cc-f2e2-489a-8fac-4279abcbbefa</td><td>John</td><td>2000</td><td>1922-06-19</td><td>2022-06-19T15:00</td></tr></table></body></html>"""
      )

      expectedResults should contain(resultBuilder.toString)
    }

    "write html with styles" in {
      val resultBuilder = new mutable.StringBuilder

      implicit val styling: HtmlStyling[PeopleStats] = new HtmlStyling[PeopleStats] {
        private val headers = Map
          .empty[String, List[Text.Modifier]]
          .withDefaultValue(
            List(backgroundColor := "red", border := "3px solid #000")
          )

        private val fields = Map(
          "isProgrammer" -> List(
            backgroundColor := "yellow",
            verticalAlign.top,
            textAlign.left,
            border := "2px solid #000"
          ),
          "birthYear" -> List(
            backgroundColor := "blue",
            verticalAlign.top,
            textAlign.left,
            border := "2px solid #000"
          ),
          "avgWorkingExperience" -> List(
            backgroundColor := "blue",
            verticalAlign.top,
            textAlign.left,
            border := "2px solid #000"
          )
        ).withDefaultValue(List(verticalAlign.top, textAlign.left, border := "2px solid #000"))

        override def headerStyle(name: String): List[Text.Modifier] = headers(name)

        override def fieldStyle(name: String): List[Text.Modifier] = fields(name)
      }

      implicit val config: HtmlTableEncoderConfig[PeopleStats] =
        HtmlTableEncoderConfig.default.copy(
          bodyTag = body(fontFamily := "Times New Roman")(
            table(tableLayout.fixed, borderCollapse.separate, width := "100%")(
              tr(
                td(p("Data Report", fontSize := "64px")),
                td(
                  img(
                    src := "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ac/Service_mark.svg/1280px-Service_mark.svg.png",
                    alt    := "Example company",
                    width  := "300px",
                    height := "130px"
                  )
                )
              )
            )
          ),
          tableTag = table(tableLayout.fixed, borderCollapse.collapse, width := "100%"),
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

      // TODO: groupBy in scala 2.12 doesn't preserve order?
      val expectedResults = List(
        """<html><head></head><body style="font-family: Times New Roman;"><table style="table-layout: fixed; border-collapse: separate; width: 100%;"><tr><td><p style="font-size: 64px;">Data Report</p></td><td><img src="https://upload.wikimedia.org/wikipedia/commons/thumb/a/ac/Service_mark.svg/1280px-Service_mark.svg.png" alt="Example company" style="width: 300px; height: 130px;" /></td></tr></table><table style="table-layout: fixed; border-collapse: collapse; width: 100%;"><tr style="border-bottom: 2px solid #ddd;"><th style="background-color: red; border: 3px solid #000;">isProgrammer</th><th style="background-color: red; border: 3px solid #000;">birthYear</th><th style="background-color: red; border: 3px solid #000;">avgWorkingExperience</th><th style="background-color: red; border: 3px solid #000;">id</th><th style="background-color: red; border: 3px solid #000;">name</th><th style="background-color: red; border: 3px solid #000;">workingExperienceYears</th><th style="background-color: red; border: 3px solid #000;">birthDay</th><th style="background-color: red; border: 3px solid #000;">createdAt</th></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; vertical-align: top; text-align: left; border: 2px solid #000;">true</td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;">1922</td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;">2000.0</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">304e27cc-f2e2-489a-8fac-4279abcbbefa</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">John</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">2000</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">1922-06-19</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">2022-06-19T15:00</td></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;">1997</td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;">100500.0</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">4ffe9631-2169-4c50-90ff-8818bc28ab3f</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">Vitalii</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">100500</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">1997-11-13</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">2022-06-19T15:00</td></tr></table></body></html>""",
        """<html><head></head><body style="font-family: Times New Roman;"><table style="table-layout: fixed; border-collapse: separate; width: 100%;"><tr><td><p style="font-size: 64px;">Data Report</p></td><td><img src="https://upload.wikimedia.org/wikipedia/commons/thumb/a/ac/Service_mark.svg/1280px-Service_mark.svg.png" alt="Example company" style="width: 300px; height: 130px;" /></td></tr></table><table style="table-layout: fixed; border-collapse: collapse; width: 100%;"><tr style="border-bottom: 2px solid #ddd;"><th style="background-color: red; border: 3px solid #000;">isProgrammer</th><th style="background-color: red; border: 3px solid #000;">birthYear</th><th style="background-color: red; border: 3px solid #000;">avgWorkingExperience</th><th style="background-color: red; border: 3px solid #000;">id</th><th style="background-color: red; border: 3px solid #000;">name</th><th style="background-color: red; border: 3px solid #000;">workingExperienceYears</th><th style="background-color: red; border: 3px solid #000;">birthDay</th><th style="background-color: red; border: 3px solid #000;">createdAt</th></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; vertical-align: top; text-align: left; border: 2px solid #000;">true</td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;">1997</td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;">100500.0</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">4ffe9631-2169-4c50-90ff-8818bc28ab3f</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">Vitalii</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">100500</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">1997-11-13</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">2022-06-19T15:00</td></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;">1922</td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;">2000.0</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">304e27cc-f2e2-489a-8fac-4279abcbbefa</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">John</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">2000</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">1922-06-19</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">2022-06-19T15:00</td></tr></table></body></html>"""
      )

      expectedResults should contain(resultBuilder.toString)
    }

  }
}
