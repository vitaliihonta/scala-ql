# ScalaQL

ScalaQL is simple statically typed (and lawful) query DSL for scala.

## Use cases

- Generating data reports
- Data exploration

## Supported sources

- Any scala collection
- CSV/TSV file
- JSON file (single line / multiline)
- [TBD] parquet

## Example

Examples could be found [here](./examples/src/main/)

```scala
import scalaql._
import com.example.Hogwarts._
import java.nio.file.Paths

object FilesExample extends App {
  case class FacultyStats(faculty: String, avgGrade: Double)

  val query: Query[From[Student] with From[Faculty], FacultyStats] =
    select[Student]
      .where(_.age >= 18)
      .join(select[Faculty])
      .on(_.faculty == _.name)
      .groupBy { case (_, faculty) => faculty.name }
      .aggregate((_, studentsWithFaculties) => studentsWithFaculties.avgBy { case (student, _) => student.grade })
      .map((FacultyStats.apply _).tupled)

  println {
    query.toList
      .run(
        from(
          csv.file[Student](path = Paths.get("examples/src/main/resources/students.csv"))
        ) & from(
          json.file[Faculty](path = Paths.get("examples/src/main/resources/faculties.json"))
        )
      )
  }
}

```
