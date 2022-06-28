package scalaql.html

import scalaql.*
import scalaql.sources.Naming
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

  case class Names(name: String, surname: String)
  case class Metadata(id: UUID, createdAt: LocalDateTime)
  case class NestedPersonOption(names: Names, metadata: Option[Metadata])

  case class PeopleStats(
    isProgrammer: Boolean,
    stats:        List[PeopleStatsPerIsProgrammer]) {

    val foo = 1
  }

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
          html.write[Person].string(resultBuilder)
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

    "write nested html with options correctly" in {
      val resultBuilder = new mutable.StringBuilder

      select[NestedPersonOption]
        .foreach(
          html.write[NestedPersonOption].string(resultBuilder)
        )
        .run(
          from(
            List(
              NestedPersonOption(
                names = Names(
                  name = "Vitalii",
                  surname = "Honta"
                ),
                metadata = Some(
                  Metadata(
                    id = UUID.fromString("4ffe9631-2169-4c50-90ff-8818bc28ab3f"),
                    createdAt = LocalDateTime.of(2022, 6, 19, 15, 0)
                  )
                )
              ),
              NestedPersonOption(
                names = Names(
                  name = "John",
                  surname = "Doe"
                ),
                metadata = None
              )
            )
          )
        )

      resultBuilder.toString shouldEqual
        """<html><head></head><body><table><tr><th>name</th><th>surname</th><th>id</th><th>createdAt</th></tr><tr><td>Vitalii</td><td>Honta</td><td>4ffe9631-2169-4c50-90ff-8818bc28ab3f</td><td>2022-06-19T15:00</td></tr><tr><td>John</td><td>Doe</td><td></td><td></td></tr></table></body></html>"""
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
          html.write[PeopleStats].string(resultBuilder)
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

      // NOTE: groupBy in scala 2.12 doesn't preserve natural order?
      val expectedResults = List(
        """<html><head></head><body><table><tr><th>isProgrammer</th><th>birthYear</th><th>avgWorkingExperience</th><th>id</th><th>name</th><th>workingExperienceYears</th><th>birthDay</th><th>createdAt</th></tr><tr><td>true</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr><tr><td></td><td>1922</td><td>2000.0</td><td></td><td></td><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td><td>304e27cc-f2e2-489a-8fac-4279abcbbefa</td><td>John</td><td>2000</td><td>1922-06-19</td><td>2022-06-19T15:00</td></tr><tr><td></td><td>1997</td><td>100500.0</td><td></td><td></td><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td><td>4ffe9631-2169-4c50-90ff-8818bc28ab3f</td><td>Vitalii</td><td>100500</td><td>1997-11-13</td><td>2022-06-19T15:00</td></tr></table></body></html>""",
        """<html><head></head><body><table><tr><th>isProgrammer</th><th>birthYear</th><th>avgWorkingExperience</th><th>id</th><th>name</th><th>workingExperienceYears</th><th>birthDay</th><th>createdAt</th></tr><tr><td>true</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr><tr><td></td><td>1997</td><td>100500.0</td><td></td><td></td><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td><td>4ffe9631-2169-4c50-90ff-8818bc28ab3f</td><td>Vitalii</td><td>100500</td><td>1997-11-13</td><td>2022-06-19T15:00</td></tr><tr><td></td><td>1922</td><td>2000.0</td><td></td><td></td><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td><td>304e27cc-f2e2-489a-8fac-4279abcbbefa</td><td>John</td><td>2000</td><td>1922-06-19</td><td>2022-06-19T15:00</td></tr></table></body></html>"""
      )

      expectedResults should contain(resultBuilder.toString)
    }

    "write html with styles" in {
      val resultBuilder = new mutable.StringBuilder

      val styling: HtmlStyling[PeopleStats] = HtmlStyling
        .builder[PeopleStats]
        .forHeader(_.isProgrammer, List(backgroundColor := "green", border := "3px solid #000"))
        .forHeader(_.stats.each.birthYear, List(backgroundColor := "yellow", border := "3px solid #000"))
        .forHeader(_.stats.each.avgWorkingExperience, List(backgroundColor := "yellow", border := "3px solid #000"))
        .forHeader(_.stats.each.records.each.id, List(backgroundColor := "orange", border := "3px solid #000"))
        .withDefaultForHeaders(List(backgroundColor := "red", border := "3px solid #000"))
        .forField(
          _.isProgrammer,
          List(
            backgroundColor := "yellow",
            fontWeight.bold,
            verticalAlign.top,
            textAlign.left,
            border := "2px solid #000"
          )
        )
        .forField(
          _.stats.each.birthYear,
          List(
            backgroundColor := "blue",
            fontWeight.bold,
            color.white,
            verticalAlign.top,
            textAlign.left,
            border := "2px solid #000"
          )
        )
        .forField(
          _.stats.each.avgWorkingExperience,
          List(
            backgroundColor := "blue",
            fontWeight.bold,
            color.white,
            verticalAlign.top,
            textAlign.left,
            border := "2px solid #000"
          )
        )
        .forField(
          _.stats.each.records.each.id,
          List(
            verticalAlign.top,
            textAlign.left,
            fontWeight.bold,
            color  := "orange",
            border := "3px solid #000"
          )
        )
        .withDefaultForFields(
          List(verticalAlign.top, textAlign.left, border := "2px solid #000")
        )
        .build

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
          html
            .write[PeopleStats]
            .option(Naming.WithSpacesCapitalize)
            .option(styling)
            .options(
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
              trTag = tr(borderBottom := "2px solid #ddd")
            )
            .string(resultBuilder)
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

      // NOTE: groupBy in scala 2.12 doesn't preserve natural order?
      val expectedResults = List(
        """<html><head></head><body style="font-family: Times New Roman;"><table style="table-layout: fixed; border-collapse: separate; width: 100%;"><tr><td><p style="font-size: 64px;">Data Report</p></td><td><img src="https://upload.wikimedia.org/wikipedia/commons/thumb/a/ac/Service_mark.svg/1280px-Service_mark.svg.png" alt="Example company" style="width: 300px; height: 130px;" /></td></tr></table><table style="table-layout: fixed; border-collapse: collapse; width: 100%;"><tr style="border-bottom: 2px solid #ddd;"><th style="background-color: green; border: 3px solid #000;">Is Programmer</th><th style="background-color: yellow; border: 3px solid #000;">Birth Year</th><th style="background-color: yellow; border: 3px solid #000;">Avg Working Experience</th><th style="background-color: orange; border: 3px solid #000;">Id</th><th style="background-color: red; border: 3px solid #000;">Name</th><th style="background-color: red; border: 3px solid #000;">Working Experience Years</th><th style="background-color: red; border: 3px solid #000;">Birth Day</th><th style="background-color: red; border: 3px solid #000;">Created At</th></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; font-weight: bold; vertical-align: top; text-align: left; border: 2px solid #000;">true</td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; font-weight: bold; color: orange; border: 3px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; font-weight: bold; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;">1922</td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;">2000.0</td><td style="vertical-align: top; text-align: left; font-weight: bold; color: orange; border: 3px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; font-weight: bold; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; font-weight: bold; color: orange; border: 3px solid #000;">304e27cc-f2e2-489a-8fac-4279abcbbefa</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">John</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">2000</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">1922-06-19</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">2022-06-19T15:00</td></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; font-weight: bold; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;">1997</td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;">100500.0</td><td style="vertical-align: top; text-align: left; font-weight: bold; color: orange; border: 3px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; font-weight: bold; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; font-weight: bold; color: orange; border: 3px solid #000;">4ffe9631-2169-4c50-90ff-8818bc28ab3f</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">Vitalii</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">100500</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">1997-11-13</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">2022-06-19T15:00</td></tr></table></body></html>""",
        """<html><head></head><body style="font-family: Times New Roman;"><table style="table-layout: fixed; border-collapse: separate; width: 100%;"><tr><td><p style="font-size: 64px;">Data Report</p></td><td><img src="https://upload.wikimedia.org/wikipedia/commons/thumb/a/ac/Service_mark.svg/1280px-Service_mark.svg.png" alt="Example company" style="width: 300px; height: 130px;" /></td></tr></table><table style="table-layout: fixed; border-collapse: collapse; width: 100%;"><tr style="border-bottom: 2px solid #ddd;"><th style="background-color: green; border: 3px solid #000;">Is Programmer</th><th style="background-color: yellow; border: 3px solid #000;">Birth Year</th><th style="background-color: yellow; border: 3px solid #000;">Avg Working Experience</th><th style="background-color: orange; border: 3px solid #000;">Id</th><th style="background-color: red; border: 3px solid #000;">Name</th><th style="background-color: red; border: 3px solid #000;">Working Experience Years</th><th style="background-color: red; border: 3px solid #000;">Birth Day</th><th style="background-color: red; border: 3px solid #000;">Created At</th></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; font-weight: bold; vertical-align: top; text-align: left; border: 2px solid #000;">true</td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; font-weight: bold; color: orange; border: 3px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; font-weight: bold; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;">1997</td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;">100500.0</td><td style="vertical-align: top; text-align: left; font-weight: bold; color: orange; border: 3px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; font-weight: bold; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; font-weight: bold; color: orange; border: 3px solid #000;">4ffe9631-2169-4c50-90ff-8818bc28ab3f</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">Vitalii</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">100500</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">1997-11-13</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">2022-06-19T15:00</td></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; font-weight: bold; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;">1922</td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;">2000.0</td><td style="vertical-align: top; text-align: left; font-weight: bold; color: orange; border: 3px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; border: 2px solid #000;"></td></tr><tr style="border-bottom: 2px solid #ddd;"><td style="background-color: yellow; font-weight: bold; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="background-color: blue; font-weight: bold; color: white; vertical-align: top; text-align: left; border: 2px solid #000;"></td><td style="vertical-align: top; text-align: left; font-weight: bold; color: orange; border: 3px solid #000;">304e27cc-f2e2-489a-8fac-4279abcbbefa</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">John</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">2000</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">1922-06-19</td><td style="vertical-align: top; text-align: left; border: 2px solid #000;">2022-06-19T15:00</td></tr></table></body></html>"""
      )

      expectedResults should contain(resultBuilder.toString)
    }

  }
}
