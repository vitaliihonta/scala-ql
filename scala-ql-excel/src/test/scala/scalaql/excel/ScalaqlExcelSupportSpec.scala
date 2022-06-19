package scalaql.excel

import scalaql.*

import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ScalaqlExcelSupportSpec extends ScalaqlUnitSpec {
  case class Person(name: String, age: Int)
  case class DetailedPerson(
    id:        UUID,
    name:      String,
    salary:    BigDecimal,
    birthDay:  LocalDate,
    createdAt: LocalDateTime)

  "ScalaqlExcelSupport" should {
    "correctly read xlsx document without headers" in {
      val path = Paths.get("scala-ql-excel/src/test/resources/without-headers.xlsx")

      select[Person].toList
        .run(
          from(
            excel.read.file[Person](path)
          )
        ) should contain theSameElementsAs {
        List(Person(name = "Vitalii", age = 24), Person(name = "John", age = 100))
      }
    }

    "correctly read xlsx document with headers" in {
      val path = Paths.get("scala-ql-excel/src/test/resources/with-headers.xlsx")

      implicit val excelConfig: ExcelConfig = ExcelConfig.default.copy(
        cellResolutionStrategy = CellResolutionStrategy.NameBased()
      )

      select[Person].toList
        .run(
          from(
            excel.read.file[Person](path)
          )
        ) should contain theSameElementsAs {
        List(Person(name = "Vitalii", age = 24), Person(name = "John", age = 100))
      }
    }

    "correctly read complex xlsx document with headers" in {
      val path = Paths.get("scala-ql-excel/src/test/resources/complex-with-headers.xlsx")

      implicit val excelConfig: ExcelConfig = ExcelConfig.default.copy(
        cellResolutionStrategy = CellResolutionStrategy.NameBased(Naming.WithSpaces)
      )

      select[DetailedPerson].toList
        .run(
          from(
            excel.read.file[DetailedPerson](path)
          )
        ) should contain theSameElementsAs {
        List(
          DetailedPerson(
            id = UUID.fromString("4ffe9631-2169-4c50-90ff-8818bc28ab3f"),
            name = "Vitalii",
            salary = BigDecimal("1005000000000000000.2"),
            birthDay = LocalDate.of(1997, 11, 13),
            createdAt = LocalDateTime.of(2022, 6, 19, 15, 0)
          ),
          DetailedPerson(
            id = UUID.fromString("304e27cc-f2e2-489a-8fac-4279abcbbefa"),
            name = "John",
            salary = BigDecimal("1005000000000.1"),
            birthDay = LocalDate.of(1922, 6, 19),
            createdAt = LocalDateTime.of(2022, 6, 19, 15, 0)
          )
        )
      }
    }
  }
}
