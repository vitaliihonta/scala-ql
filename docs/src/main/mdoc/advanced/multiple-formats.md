# Reading data with multiple data formats

With Scala QL, it's really easy to run a `Query` on data of different kind and format.  

Start by importing:

```scala mdoc
import scalaql._

// Docs classes
import scalaql.docs.DocUtils._
import scalaql.docs.Hogwarts._

// Used in examples
import java.nio.file.Paths
import io.circe.generic.auto._
```

Assume you have the following aggregation `Query`:
```scala mdoc
case class FacultyStats(faculty: String, avgGrade: Double)

val query =
  select[Student]
    .where(_.age >= 18)
    .join(select[Faculty])
    .on(_.faculty == _.name)
    .groupBy { case (_, faculty) => faculty.name }
    .aggregate { (faculty, studentsWithFaculties) =>
      studentsWithFaculties
        .avgBy { case (student, _) => student.grade }
        .map(avgGrade => FacultyStats(faculty, avgGrade))
    }
```

You could run this `Query` through CSV and JSON files at the same time!  
Let's start by defining input files paths:

```scala mdoc
val studentsPath  = Paths.get("docs/src/main/resources/students.csv")
val facultiesPath = Paths.get("docs/src/main/resources/faculties.json")
  
val outPath = Paths.get("target/faculty_stats_complex.csv")
```

Then you only need to just provide the correct input:

```scala mdoc
query
  .foreach(
    csv.write[FacultyStats].file(outPath)
  )
  .run(
    from(
      csv.read[Student].file(studentsPath)
    ) & from(
      json.read[Faculty].file(facultiesPath)
    )
  )
```

It will produce the following CSV file:
```scala mdoc
printFile(outPath)
```