package scalaql.html

import scalaql.ScalaqlUnitSpec
import scalatags.Text.TypedTag
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import scalatags.Text.all.*

class HtmlTableEncoderAutoDerivationSpec extends ScalaqlUnitSpec {
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
    avgExperience: Double,
    records:       List[PersonRecord])

  case class PersonRecord(
    id:                     UUID,
    name:                   String,
    workingExperienceYears: Int,
    birthDay:               LocalDate,
    createdAt:              LocalDateTime)

  private def renderTable(
    encoder:      HtmlTableEncoder[?]
  )(data:         Modifier
  )(implicit ctx: HtmlTableEncoderContext
  ): String =
    table(
      tr(
        encoder.headers.map(h => th(h))
      ),
      data
    ).toString

  "HtmlTableEncoder" should {
    "generate correctly html table" in {
      val encoder = HtmlTableEncoder[Person]
      implicit val context: HtmlTableEncoderContext = HtmlTableEncoderContext.initial(
        headers = encoder.headers,
        fieldStyles = _ => Nil
      )

      renderTable(encoder) {
        encoder
          .write(
            Person(
              id = UUID.fromString("2769a48d-8fec-4242-81d1-959ae424712c"),
              name = "Vitalii",
              workingExperienceYears = 100500,
              birthDay = LocalDate.of(1997, 11, 13),
              createdAt = LocalDateTime.of(2022, 6, 15, 12, 55, 0),
              isProgrammer = true
            )
          )
          .value
      } shouldEqual """<table><tr><th>id</th><th>name</th><th>workingExperienceYears</th><th>birthDay</th><th>createdAt</th><th>isProgrammer</th></tr><tr><td>2769a48d-8fec-4242-81d1-959ae424712c</td><td>Vitalii</td><td>100500</td><td>1997-11-13</td><td>2022-06-15T12:55</td><td>true</td></tr></table>"""
    }

    "generate correctly nested html table" in {
      val encoder = HtmlTableEncoder[PeopleStats]
      implicit val context: HtmlTableEncoderContext = HtmlTableEncoderContext.initial(
        headers = encoder.headers,
        fieldStyles = _ => Nil
      )

      renderTable(encoder) {
        encoder
          .write(
            PeopleStats(
              isProgrammer = true,
              stats = List(
                PeopleStatsPerIsProgrammer(
                  avgExperience = 100500,
                  records = List(
                    PersonRecord(
                      id = UUID.fromString("2769a48d-8fec-4242-81d1-959ae424712c"),
                      name = "Vitalii",
                      workingExperienceYears = 100498,
                      birthDay = LocalDate.of(1997, 11, 13),
                      createdAt = LocalDateTime.of(2022, 6, 15, 12, 55, 0)
                    ),
                    PersonRecord(
                      id = UUID.fromString("2769a48d-8fec-4242-81d1-959ae424711c"),
                      name = "Vitali2",
                      workingExperienceYears = 100502,
                      birthDay = LocalDate.of(1997, 11, 13),
                      createdAt = LocalDateTime.of(2022, 6, 15, 12, 55, 0)
                    )
                  )
                ),
                PeopleStatsPerIsProgrammer(
                  avgExperience = 100500,
                  records = List(
                    PersonRecord(
                      id = UUID.fromString("304e27cc-f2e2-489a-8fac-4279abcbbefa"),
                      name = "John",
                      workingExperienceYears = 10050,
                      birthDay = LocalDate.of(1922, 6, 19),
                      createdAt = LocalDateTime.of(2022, 6, 19, 15, 0)
                    )
                  )
                )
              )
            )
          )
          .value
      } shouldEqual """<table><tr><th>isProgrammer</th><th>avgExperience</th><th>id</th><th>name</th><th>workingExperienceYears</th><th>birthDay</th><th>createdAt</th></tr><tr><td>true</td><td></td><tr><td></td><td>100500.0</td><tr><td></td><td></td><td>2769a48d-8fec-4242-81d1-959ae424712c</td><td>Vitalii</td><td>100498</td><td>1997-11-13</td><td>2022-06-15T12:55</td></tr><tr><td></td><td></td><td>2769a48d-8fec-4242-81d1-959ae424711c</td><td>Vitali2</td><td>100502</td><td>1997-11-13</td><td>2022-06-15T12:55</td></tr><td></td><td></td><td></td><td></td><td></td></tr><tr><td></td><td>100500.0</td><tr><td></td><td></td><td>304e27cc-f2e2-489a-8fac-4279abcbbefa</td><td>John</td><td>10050</td><td>1922-06-19</td><td>2022-06-19T15:00</td></tr><td></td><td></td><td></td><td></td><td></td></tr><td></td><td></td><td></td><td></td><td></td></tr></table>"""
    }
  }
}
