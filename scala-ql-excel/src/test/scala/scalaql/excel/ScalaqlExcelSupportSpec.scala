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

  case class DetailedPersonWithMissingFields(
    id:             UUID,
    name:           String,
    salary:         BigDecimal,
    birthDay:       LocalDate,
    createdAt:      LocalDateTime,
    missingBoolean: Boolean,
    missingString:  String)

  case class DetailedPersonWithFormulas(
    name:     String,
    surname:  String,
    fullName: String)

  case class Names(name: String, surname: String)
  case class Metadata(id: UUID, createdAt: LocalDateTime)

  case class NestedPerson(names: Names, metadata: Metadata)
  case class NestedPersonOption(names: Names, metadata: Option[Metadata])
  case class NestedPersonOrderSensitive(metadata: Metadata, names: Names)

  case class PersonWithProfession(
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

  "ExcelDecoder" should {
    "correctly read xlsx document without headers" in {
      val path = Paths.get("scala-ql-excel/src/test/resources/without-headers.xlsx")

      select[Person].toList
        .run(
          from(
            excel.read[Person].file(path)
          )
        ) should contain theSameElementsAs {
        List(Person(name = "Vitalii", age = 24), Person(name = "John", age = 100))
      }
    }

    "correctly read xlsx document with headers" in {
      val path = Paths.get("scala-ql-excel/src/test/resources/with-headers.xlsx")

      select[Person].toList
        .run(
          from(
            excel
              .read[Person]
              .option(CellResolutionStrategy.NameBased)
              .file(path)
          )
        ) should contain theSameElementsAs {
        List(Person(name = "Vitalii", age = 24), Person(name = "John", age = 100))
      }
    }

    "correctly read nested xlsx document with options and headers" in {
      val path = Paths.get("scala-ql-excel/src/test/resources/nested-options-with-headers.xls")

      select[NestedPersonOption].toList
        .run(
          from(
            excel
              .read[NestedPersonOption]
              .option(Naming.WithSpacesLowerCase)
              .option(CellResolutionStrategy.NameBased)
              .file(path)
          )
        ) should contain theSameElementsAs {
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
      }
    }

    "correctly read complex xlsx document with headers" in {
      val path = Paths.get("scala-ql-excel/src/test/resources/complex-with-headers.xlsx")

      select[DetailedPerson].toList
        .run(
          from(
            excel
              .read[DetailedPerson]
              .option(Naming.WithSpacesLowerCase)
              .option(CellResolutionStrategy.NameBased)
              .file(path)
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

    "correctly read complex xlsx document handling errors" in {
      val path = Paths.get("scala-ql-excel/src/test/resources/errors-complex-with-headers.xlsx")

      val caught = intercept[ExcelDecoderException.Accumulating] {
        select[DetailedPerson].toList
          .run(
            from(
              excel
                .read[DetailedPerson]
                .option(Naming.WithSpacesLowerCase)
                .option(CellResolutionStrategy.NameBased)
                .file(path)
            )
          )
      }

      caught.toString shouldBe
        """scalaql.excel.ExcelDecoderException$Accumulating: Failed to decode DetailedPerson (at `root`):
          |	+ ( scalaql.excel.ExcelDecoderException$CannotDecode: Cannot decode cell of row number #0 at path `id`: java.lang.IllegalArgumentException: Invalid UUID string: foo )
          |	+ ( scalaql.excel.ExcelDecoderException$CannotDecode: Cannot decode cell of row number #0 at path `birthDay`: java.time.format.DateTimeParseException: Text 'xxx' could not be parsed at index 0 )
          |""".stripMargin
    }

    "correctly read complex xlsx document handling missing cells errors" in {
      val path = Paths.get("scala-ql-excel/src/test/resources/errors-complex-with-headers.xlsx")

      val caught = intercept[ExcelDecoderException.Accumulating] {
        select[DetailedPersonWithMissingFields].toList
          .run(
            from(
              excel
                .read[DetailedPersonWithMissingFields]
                .option(Naming.WithSpacesLowerCase)
                .option(CellResolutionStrategy.NameBased)
                .file(path)
            )
          )
      }

      caught.toString shouldEqual
        """scalaql.excel.ExcelDecoderException$Accumulating: Failed to decode DetailedPersonWithMissingFields (at `root`):
          |	+ ( scalaql.excel.ExcelDecoderException$CannotDecode: Cannot decode cell of row number #0 at path `id`: java.lang.IllegalArgumentException: Invalid UUID string: foo )
          |	+ ( scalaql.excel.ExcelDecoderException$CannotDecode: Cannot decode cell of row number #0 at path `birthDay`: java.time.format.DateTimeParseException: Text 'xxx' could not be parsed at index 0 )
          |	+ ( scalaql.excel.ExcelDecoderException$FieldNotFound: Field not found for row number #0 at path `missingBoolean` )
          |	+ ( scalaql.excel.ExcelDecoderException$FieldNotFound: Field not found for row number #0 at path `missingString` )
          |""".stripMargin
    }

    "correctly read nested xlsx document with headers" in {
      val path = Paths.get("scala-ql-excel/src/test/resources/nested-with-headers.xlsx")

      select[NestedPerson].toList
        .run(
          from(
            excel
              .read[NestedPerson]
              .option(Naming.WithSpacesLowerCase)
              .option(CellResolutionStrategy.NameBased)
              .file(path)
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
            excel.read[NestedPersonOrderSensitive].file(path)
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

      select[DetailedPersonWithFormulas].toList
        .run(
          from(
            excel
              .read[DetailedPersonWithFormulas]
              .option(evaluateFormulas = true)
              .option(Naming.WithSpacesLowerCase)
              .option(CellResolutionStrategy.NameBased)
              .file(path)
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
  }

  "ExcelEncoder" should {
    "correctly write simple xls without headers" in {
      val path = Files.createTempFile(Paths.get("scala-ql-excel/src/test/out/"), "write", "without-headers.xls")
      select[Person]
        .foreach(
          excel.write[Person].file(path)
        )
        .run(
          from(
            List(Person(name = "Vitalii", age = 24), Person(name = "John", age = 100))
          )
        )

      assertWorkbooksEqual(path, Paths.get("scala-ql-excel/src/test/expected/write-without-headers.xls"))
      deleteFile(path)
    }

    "correctly write simple xls with headers" in {
      val path = Files.createTempFile(Paths.get("scala-ql-excel/src/test/out/"), "write", "with-headers.xls")

      select[Person]
        .foreach(
          excel
            .write[Person]
            .option(headers = true)
            .option(Naming.WithSpacesLowerCase)
            .file(path)
        )
        .run(
          from(
            List(Person(name = "Vitalii", age = 24), Person(name = "John", age = 100))
          )
        )

      assertWorkbooksEqual(path, Paths.get("scala-ql-excel/src/test/expected/write-with-headers.xls"))
      deleteFile(path)
    }

    "correctly write simple xls with headers and styles" in {
      val path = Files.createTempFile(Paths.get("scala-ql-excel/src/test/out/"), "write-styles", "with-headers.xls")

      val personStyling: ExcelStyling[Person] = ExcelStyling
        .builder[Person]
        .forAllHeaders(
          cellStyle
            .andThen(_.setFillPattern(FillPatternType.SOLID_FOREGROUND))
            .andThen(_.setFillForegroundColor(IndexedColors.PINK.index))
        )
        .forField(_.name, cellStyle.andThen(_.setRotation(90)))
        .forField(
          _.age,
          cellStyle
            .andThen(_.setFillPattern(FillPatternType.SOLID_FOREGROUND))
            .andThen(_.setFillForegroundColor(IndexedColors.BLUE.index))
        )
        .build

      select[Person]
        .foreach(
          excel
            .write[Person]
            .option(headers = true)
            .option(Naming.WithSpacesLowerCase)
            .option(personStyling)
            .file(path)
        )
        .run(
          from(
            List(Person(name = "Vitalii", age = 24), Person(name = "John", age = 100))
          )
        )

      assertWorkbooksEqual(path, Paths.get("scala-ql-excel/src/test/expected/write-styles-with-headers.xls"))
      deleteFile(path)
    }

    "correctly write complex xlsx document with headers" in {
      val path = Files.createTempFile(Paths.get("scala-ql-excel/src/test/out/"), "write-complex", "with-headers.xls")

      select[DetailedPerson]
        .foreach(
          excel
            .write[DetailedPerson]
            .option(headers = true)
            .option(Naming.WithSpacesLowerCase)
            .file(path)
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
      deleteFile(path)
    }

    "correctly write nested xlsx document with headers" in {
      val path = Files.createTempFile(Paths.get("scala-ql-excel/src/test/out/"), "write-nested", "with-headers.xls")

      val styling: ExcelStyling[NestedPersonOption] = ExcelStyling
        .builder[NestedPersonOption]
        .forAllHeaders(
          cellStyle
            .andThen(_.setFillPattern(FillPatternType.SOLID_FOREGROUND))
            .andThen(_.setFillForegroundColor(IndexedColors.PINK.index))
        )
        .forField(_.names.name, cellStyle.andThen(_.setRotation(90)))
        .forField(_.names.surname, cellStyle.andThen(_.setRotation(-90)))
        .forField(
          _.metadata.each.id,
          cellStyle
            .andThen(_.setFillPattern(FillPatternType.SOLID_FOREGROUND))
            .andThen(_.setFillForegroundColor(IndexedColors.BLUE.index))
        )
        .build

      select[NestedPersonOption]
        .foreach(
          excel
            .write[NestedPersonOption]
            .option(headers = true)
            .option(Naming.WithSpacesLowerCase)
            .option(styling)
            .file(path)
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

      assertWorkbooksEqual(path, Paths.get("scala-ql-excel/src/test/expected/write-nested-with-headers.xls"))
      deleteFile(path)
    }

    "correctly write nested xlsx document with options and headers" in {
      val path =
        Files.createTempFile(Paths.get("scala-ql-excel/src/test/out/"), "write-nested-options", "with-headers.xls")

      select[NestedPersonOption]
        .foreach(
          excel
            .write[NestedPersonOption]
            .option(headers = true)
            .option(Naming.WithSpacesLowerCase)
            .file(path)
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

      assertWorkbooksEqual(path, Paths.get("scala-ql-excel/src/test/expected/write-nested-options-with-headers.xls"))
      deleteFile(path)
    }

    "correctly write nested xls document with list and headers" in {
      val path =
        Files.createTempFile(Paths.get("scala-ql-excel/src/test/out/"), "write-list-nested", "with-headers.xls")

      select[PersonWithProfession]
        .groupBy(_.isProgrammer)
        .aggregate { (isProgrammer, people) =>
          people
            .report(_.birthDay.getYear) { (birthYear, people) =>
              (
                people.avgBy(_.workingExperienceYears.toDouble) &&
                  people.toList
              ).map { case (avgWorkingExperienceYears, people) =>
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
            }
            .map(PeopleStats(isProgrammer, _))
        }
        .foreach(
          excel
            .write[PeopleStats]
            .option(headers = true)
            .option(Naming.WithSpacesLowerCase)
            .file(path)
        )
        .run(
          from(
            List(
              PersonWithProfession(
                id = UUID.fromString("4ffe9631-2169-4c50-90ff-8818bc28ab3f"),
                name = "Vitalii",
                workingExperienceYears = 100500,
                birthDay = LocalDate.of(1997, 11, 13),
                createdAt = LocalDateTime.of(2022, 6, 19, 15, 0),
                isProgrammer = true
              ),
              PersonWithProfession(
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

      assertWorkbooksEqualToOneOf(
        path,
        Paths.get("scala-ql-excel/src/test/expected/write-list-nested-with-headers.xls"),
        Paths.get("scala-ql-excel/src/test/expected/write-list-nested-with-headers2_12.xls")
      )

      deleteFile(path)
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

  private def assertWorkbooksEqualToOneOf(left: Path, first: Path, rest: Path*) = {
    val expected = first :: rest.toList
    val result = expected.foldLeft(false) {
      case (succeeded @ true, _) => succeeded
      case (_, path) =>
        try {
          assertWorkbooksEqual(left, path)
          true
        } catch {
          case e: org.scalatest.exceptions.TestFailedException =>
            false
        }
    }
    assert(result, s"File in $left doesn't equal to any of files in $expected")
  }

  private def deleteFile(path: Path): Unit =
    Files.deleteIfExists(path)
}
