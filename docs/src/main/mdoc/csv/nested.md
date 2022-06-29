# Writing nested case classes

ScalaQL allows you to write nested case classes into CSV files.
It will automatically flatten headers and rows when writing to a file.

Start by importing `scalaql`:

```scala mdoc
import scalaql._

// Docs classes
import scalaql.docs.Hogwarts._
import scalaql.docs.DocUtils._

// Imports for examples
import java.nio.file.Paths
```

For instance, assume you have the following case class:

```scala mdoc
case class FacultyDescription(name: String)

case class FacultyInfoStats(
  avgAge: Double, 
  totalGrade: Double
)
case class FacultyInfoNested(
  description: FacultyDescription, 
  stats: FacultyInfoStats)
```

Then with such `Query`:

```scala mdoc
val nestedAggregation: Query[From[Student], FacultyInfoNested] =
  select[Student]
    .groupBy(_.faculty)
    .aggregate((faculty, students) =>
      (
        students.avgBy(_.age.toDouble) &&
        students.sumBy(_.grade)
      ).map { case (avgAge, totalGrade) =>
        FacultyInfoNested(
          FacultyDescription(faculty),
          FacultyInfoStats(avgAge, totalGrade)
        )      
      }
    )
```

It will produce the following CSV file:

```scala mdoc
val studentsPath = Paths.get("docs/src/main/resources/students.csv")
val nestedPath = Paths.get("docs/target/stats_nested.csv")

nestedAggregation
  .foreach(
    csv.write[FacultyInfoNested].file(nestedPath)
  )
  .run(
    from(
      csv.read[Student].file(studentsPath)
    )
  )
  
printFile(nestedPath)
```
