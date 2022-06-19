package scalaql.excel

import org.apache.poi.ss.usermodel.CellStyle
import scalaql.*

import java.time.LocalDate

class ExcelStylingBuilderSpec extends ScalaqlUnitSpec {
  case class Person(name: String, birthDay: LocalDate)

  "ExcelStylingBuilder" should {
    "build correctly" in {

      println {
        ExcelStyling
          .builder[Person]
          .forField(_.name, _.setRotation(10))
          .build
      }
    }
  }
}
