package scalaql.describe

import scalaql.*

import java.time.LocalDate

class DescribeSpec extends ScalaqlUnitSpec {
  case class Student(
    name:           String,
    age:            Int,
    faculty:        String,
    grade:          Double,
    specialization: String,
    birthDay:       LocalDate)

  private val Harry: Student = Student(
    name = "Harry",
    age = 19,
    faculty = "Gryffindor",
    grade = 85.1,
    specialization = "learning",
    birthDay = LocalDate.of(1980, 7, 31)
  )

  private val Ron: Student = Student(
    name = "Ron",
    age = 18,
    faculty = "Gryffindor",
    grade = 66.2,
    specialization = "eating",
    birthDay = LocalDate.of(1980, 5, 1)
  )

  private val Hermione: Student = Student(
    name = "Hermione",
    age = 18,
    faculty = "Gryffindor",
    grade = 99.6,
    specialization = "learning",
    birthDay = LocalDate.of(1979, 9, 17)
  )

  private val Draco: Student = Student(
    name = "Draco",
    age = 18,
    faculty = "Slytherin",
    grade = 85.1,
    specialization = "trolling",
    birthDay = LocalDate.of(1980, 6, 5)
  )

  private val Cedric: Student = Student(
    name = "Cedric",
    age = 17,
    faculty = "Hufflepuff",
    grade = 90.1,
    specialization = "young dying",
    birthDay = LocalDate.of(1977, 10, 1)
  )

  private val students: List[Student] = List(Harry, Ron, Hermione, Draco, Cedric)

  "Describe" should {
    "provide correct statistics" in {
      val actualResult = select[Student]
        .describe()
        .toList
        .run(from(students))

      val expectedResult = List(
        RowDescription(
          field = "birthDay",
          count = 5,
          mean = None,
          std = None,
          min = Some(LocalDate.of(1977, 10, 1).toString),
          percentile25 = None,
          percentile75 = None,
          percentile90 = None,
          max = Some(LocalDate.of(1980, 7, 31).toString),
          unique = Set.empty
        ),
        RowDescription(
          field = "grade",
          count = 5,
          mean = Some("85.22"),
          std = Some("10.88474"),
          min = Some("66.2"),
          percentile25 = Some("85.1"),
          percentile75 = Some("90.1"),
          percentile90 = Some("99.6"),
          max = Some("99.6"),
          unique = Set.empty
        ),
        RowDescription(
          field = "name",
          count = 5,
          mean = None,
          std = None,
          min = None,
          percentile25 = None,
          percentile75 = None,
          percentile90 = None,
          max = None,
          unique = Set.empty
        ),
        RowDescription(
          field = "specialization",
          count = 5,
          mean = None,
          std = None,
          min = None,
          percentile25 = None,
          percentile75 = None,
          percentile90 = None,
          max = None,
          unique = Set.empty
        ),
        RowDescription(
          field = "age",
          count = 5,
          mean = Some("18.0"),
          std = Some("0.6324555"),
          min = Some("17.0"),
          percentile25 = Some("18.0"),
          percentile75 = Some("18.0"),
          percentile90 = Some("19.0"),
          max = Some("19.0"),
          unique = Set.empty
        ),
        RowDescription(
          field = "faculty",
          count = 5,
          mean = None,
          std = None,
          min = None,
          percentile25 = None,
          percentile75 = None,
          percentile90 = None,
          max = None,
          unique = Set.empty
        )
      )

      actualResult should contain theSameElementsAs {
        expectedResult
      }
    }

    "provide correct statistics with unique" in {
      val actualResult = select[Student]
        .describe(unique = true)
        .toList
        .run(from(students))

      val expectedResult = List(
        RowDescription(
          field = "birthDay",
          count = 5,
          mean = None,
          std = None,
          min = Some(LocalDate.of(1977, 10, 1).toString),
          percentile25 = None,
          percentile75 = None,
          percentile90 = None,
          max = Some(LocalDate.of(1980, 7, 31).toString),
          unique = Set.empty
        ),
        RowDescription(
          field = "grade",
          count = 5,
          mean = Some("85.22"),
          std = Some("10.88474"),
          min = Some("66.2"),
          percentile25 = Some("85.1"),
          percentile75 = Some("90.1"),
          percentile90 = Some("99.6"),
          max = Some("99.6"),
          unique = Set.empty
        ),
        RowDescription(
          field = "name",
          count = 5,
          mean = None,
          std = None,
          min = None,
          percentile25 = None,
          percentile75 = None,
          percentile90 = None,
          max = None,
          unique = Set("Ron", "Harry", "Cedric", "Draco", "Hermione")
        ),
        RowDescription(
          field = "specialization",
          count = 5,
          mean = None,
          std = None,
          min = None,
          percentile25 = None,
          percentile75 = None,
          percentile90 = None,
          max = None,
          unique = Set("learning", "eating", "trolling", "young dying")
        ),
        RowDescription(
          field = "age",
          count = 5,
          mean = Some("18.0"),
          std = Some("0.6324555"),
          min = Some("17.0"),
          percentile25 = Some("18.0"),
          percentile75 = Some("18.0"),
          percentile90 = Some("19.0"),
          max = Some("19.0"),
          unique = Set.empty
        ),
        RowDescription(
          field = "faculty",
          count = 5,
          mean = None,
          std = None,
          min = None,
          percentile25 = None,
          percentile75 = None,
          percentile90 = None,
          max = None,
          unique = Set("Gryffindor", "Slytherin", "Hufflepuff")
        )
      )

      actualResult should contain theSameElementsAs {
        expectedResult
      }
    }
  }
}
