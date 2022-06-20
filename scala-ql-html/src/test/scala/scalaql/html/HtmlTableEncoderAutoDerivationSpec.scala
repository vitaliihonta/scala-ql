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
  )(data:         List[Map[String, TypedTag[String]]]
  )(implicit ctx: HtmlTableEncoderContext
  ): TypedTag[String] = {
    val headersOrder = encoder.headers.zipWithIndex.toMap
    table(
      tr(
        encoder.headers.map(h => th(ctx.headerStyles(h))(h))
      ),
      data.map { vs =>
        tr(
          vs.toList
            .sortBy { case (n, _) => headersOrder(n) }
            .map { case (_, v) => v }
        )
      }
    )
  }

  "HtmlTableEncoder" should {
    "generate correctly html table" in {
      implicit val context: HtmlTableEncoderContext = HtmlTableEncoderContext.initial(
        head = head(),
        nestingStrategy = NestingStrategy.Flatten,
        bodyStyles = Nil,
        headerStyles = _ => Nil,
        fieldStyles = _ => Nil
      )

      println {
        renderTable(HtmlTableEncoder[Person]) {
          HtmlTableEncoder[Person]
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
        }
      }
    }

    "generate correctly nested html table" in {
      implicit val context: HtmlTableEncoderContext = HtmlTableEncoderContext.initial(
        head = head(),
        nestingStrategy = new NestingStrategy.FillGaps(td()),
        bodyStyles = Nil,
        headerStyles = _ => Nil,
        fieldStyles = _ => Nil
      )

      println {
        renderTable(HtmlTableEncoder[PeopleStats]) {
          HtmlTableEncoder[PeopleStats].write(
            PeopleStats(
              isProgrammer = true,
              stats = List(
                PeopleStatsPerIsProgrammer(
                  avgExperience = 100500,
                  records = List(
                    PersonRecord(
                      id = UUID.fromString("2769a48d-8fec-4242-81d1-959ae424712c"),
                      name = "Vitalii",
                      workingExperienceYears = 100500,
                      birthDay = LocalDate.of(1997, 11, 13),
                      createdAt = LocalDateTime.of(2022, 6, 15, 12, 55, 0)
                    )
                  )
                )
              )
            )
          )
        }
      }
    }
  }
}
