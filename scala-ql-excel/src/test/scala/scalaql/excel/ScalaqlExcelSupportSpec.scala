package scalaql.excel

import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import scalaql.*

import scala.jdk.CollectionConverters.*
import java.nio.file.Files
import java.nio.file.Path
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

  case class DetailedPersonWithFormulas(
    name:     String,
    surname:  String,
    fullName: String)

  case class Names(name: String, surname: String)
  case class Metadata(id: UUID, createdAt: LocalDateTime)

  case class NestedPerson(names: Names, metadata: Metadata)
  case class NestedPersonOrderSensitive(metadata: Metadata, names: Names)

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

      implicit val excelConfig: ExcelReadConfig = ExcelReadConfig.default.copy(
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

      implicit val excelConfig: ExcelReadConfig = ExcelReadConfig.default.copy(
        cellResolutionStrategy = CellResolutionStrategy.NameBased(Naming.WithSpacesLowerCase)
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

    "correctly read nested xlsx document with headers" in {
      val path = Paths.get("scala-ql-excel/src/test/resources/nested-with-headers.xlsx")

      implicit val excelConfig: ExcelReadConfig = ExcelReadConfig.default.copy(
        cellResolutionStrategy = CellResolutionStrategy.NameBased(Naming.WithSpacesLowerCase)
      )

      select[NestedPerson].toList
        .run(
          from(
            excel.read.file[NestedPerson](path)
          )
        ) should contain theSameElementsAs {
        List(
          NestedPerson(
            names = Names(
              name = "Vitalii",
              surname = "Honta"
            ),
            metadata = Metadata(
              id = UUID.fromString("4ffe9631-2169-4c50-90ff-8818bc28ab3f"),
              createdAt = LocalDateTime.of(2022, 6, 19, 15, 0)
            )
          ),
          NestedPerson(
            names = Names(
              name = "John",
              surname = "Doe"
            ),
            metadata = Metadata(
              id = UUID.fromString("304e27cc-f2e2-489a-8fac-4279abcbbefa"),
              createdAt = LocalDateTime.of(2022, 6, 19, 15, 0)
            )
          )
        )
      }
    }

    "correctly read nested xlsx document without headers" in {
      val path = Paths.get("scala-ql-excel/src/test/resources/nested-without-headers.xlsx")

      select[NestedPersonOrderSensitive].toList
        .run(
          from(
            excel.read.file[NestedPersonOrderSensitive](path)
          )
        ) should contain theSameElementsAs {
        List(
          NestedPersonOrderSensitive(
            metadata = Metadata(
              id = UUID.fromString("4ffe9631-2169-4c50-90ff-8818bc28ab3f"),
              createdAt = LocalDateTime.of(2022, 6, 19, 15, 0)
            ),
            names = Names(
              name = "Vitalii",
              surname = "Honta"
            )
          ),
          NestedPersonOrderSensitive(
            metadata = Metadata(
              id = UUID.fromString("304e27cc-f2e2-489a-8fac-4279abcbbefa"),
              createdAt = LocalDateTime.of(2022, 6, 19, 15, 0)
            ),
            names = Names(
              name = "John",
              surname = "Doe"
            )
          )
        )
      }
    }

    "correctly read complex xlsx document with headers and formulas" in {
      val path = Paths.get("scala-ql-excel/src/test/resources/with-headers-and-formulas.xlsx")

      implicit val excelConfig: ExcelReadConfig = ExcelReadConfig.default.copy(
        evaluateFormulas = true,
        cellResolutionStrategy = CellResolutionStrategy.NameBased(Naming.WithSpacesLowerCase)
      )

      select[DetailedPersonWithFormulas].toList
        .run(
          from(
            excel.read.file[DetailedPersonWithFormulas](path)
          )
        ) should contain theSameElementsAs {
        List(
          DetailedPersonWithFormulas(
            name = "Vitalii",
            surname = "Honta",
            fullName = "Vitalii Honta"
          ),
          DetailedPersonWithFormulas(
            name = "John",
            surname = "Doe",
            fullName = "John Doe"
          )
        )
      }
    }

    "correctly write simple xls without headers" in {
      val path = Files.createTempFile(Paths.get("scala-ql-excel/src/test/out/"), "write", "without-headers.xls")
      select[Person]
        .foreach(
          excel.write.file[Person](path)
        )
        .run(
          from(
            List(Person(name = "Vitalii", age = 24), Person(name = "John", age = 100))
          )
        )

      assertWorkbooksEqual(path, Paths.get("scala-ql-excel/src/test/expected/write-without-headers.xls"))
    }

    "correctly write simple xls with headers" in {
      val path = Files.createTempFile(Paths.get("scala-ql-excel/src/test/out/"), "write", "with-headers.xls")
      implicit val excelConfig: ExcelWriteConfig[Person] = ExcelWriteConfig.default.copy(
        writeHeaders = true,
        naming = Naming.WithSpacesLowerCase
      )
      select[Person]
        .foreach(
          excel.write.file[Person](path)
        )
        .run(
          from(
            List(Person(name = "Vitalii", age = 24), Person(name = "John", age = 100))
          )
        )

      assertWorkbooksEqual(path, Paths.get("scala-ql-excel/src/test/expected/write-with-headers.xls"))
    }

    "correctly write simple xls with headers and styles" in {
      val path = Files.createTempFile(Paths.get("scala-ql-excel/src/test/out/"), "write-styles", "with-headers.xls")

      implicit val personStyling: ExcelStyling[Person] = ExcelStyling
        .builder[Person]
        .forAllHeaders(
          cellStyle
            .andThen(_.setFillPattern(FillPatternType.SOLID_FOREGROUND))
            .andThen(_.setFillForegroundColor(IndexedColors.PINK.index))
        )
        .forAllFields(
          "name" -> cellStyle.andThen(_.setRotation(90)),
          "age" -> cellStyle
            .andThen(_.setFillPattern(FillPatternType.SOLID_FOREGROUND))
            .andThen(_.setFillForegroundColor(IndexedColors.BLUE.index))
        )
        .build

      println(personStyling)

      implicit val excelConfig: ExcelWriteConfig[Person] = ExcelWriteConfig.default
        .copy(
          writeHeaders = true,
          naming = Naming.WithSpacesLowerCase
        )

      select[Person]
        .foreach(
          excel.write.file[Person](path)
        )
        .run(
          from(
            List(Person(name = "Vitalii", age = 24), Person(name = "John", age = 100))
          )
        )

      assertWorkbooksEqual(path, Paths.get("scala-ql-excel/src/test/expected/write-styles-with-headers.xls"))
    }

    "correctly write complex xlsx document with headers" in {
      val path = Files.createTempFile(Paths.get("scala-ql-excel/src/test/out/"), "write-complex", "with-headers.xls")
      implicit val excelConfig: ExcelWriteConfig[DetailedPerson] = ExcelWriteConfig.default.copy(
        writeHeaders = true,
        naming = Naming.WithSpacesLowerCase
      )

      select[DetailedPerson]
        .foreach(
          excel.write.file[DetailedPerson](path)
        )
        .run(
          from(
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
          )
        )

      assertWorkbooksEqual(path, Paths.get("scala-ql-excel/src/test/expected/write-complex-with-headers.xls"))
    }

    "correctly write nested xlsx document with headers" in {
      val path = Files.createTempFile(Paths.get("scala-ql-excel/src/test/out/"), "write-nested", "with-headers.xls")

      implicit val excelConfig: ExcelWriteConfig[NestedPerson] = ExcelWriteConfig.default.copy(
        writeHeaders = true,
        naming = Naming.WithSpacesLowerCase
      )

      select[NestedPerson]
        .foreach(
          excel.write.file[NestedPerson](path)
        )
        .run(
          from(
            List(
              NestedPerson(
                names = Names(
                  name = "Vitalii",
                  surname = "Honta"
                ),
                metadata = Metadata(
                  id = UUID.fromString("4ffe9631-2169-4c50-90ff-8818bc28ab3f"),
                  createdAt = LocalDateTime.of(2022, 6, 19, 15, 0)
                )
              ),
              NestedPerson(
                names = Names(
                  name = "John",
                  surname = "Doe"
                ),
                metadata = Metadata(
                  id = UUID.fromString("304e27cc-f2e2-489a-8fac-4279abcbbefa"),
                  createdAt = LocalDateTime.of(2022, 6, 19, 15, 0)
                )
              )
            )
          )
        )

      assertWorkbooksEqual(path, Paths.get("scala-ql-excel/src/test/expected/write-nested-with-headers.xls"))
    }
  }

  private def assertWorkbooksEqual(left: Path, right: Path) = {
    val workbookLeft  = new XSSFWorkbook(left.toFile)
    val workbookRight = new XSSFWorkbook(right.toFile)

    val leftSheets  = workbookLeft.sheetIterator().asScala.toList
    val rightSheets = workbookRight.sheetIterator().asScala.toList

    assert(leftSheets.size == rightSheets.size)

    for ((leftSheet, rightSheet) <- leftSheets zip rightSheets) {
      val leftRows  = leftSheet.rowIterator().asScala.toList
      val rightRows = rightSheet.rowIterator().asScala.toList
      assert(leftRows.size == rightRows.size)
      for ((leftRow, rightRow) <- leftRows zip rightRows)
        assert(leftRow.toString == rightRow.toString)
    }
  }
}
