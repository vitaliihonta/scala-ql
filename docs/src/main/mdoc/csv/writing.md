# Writing CSV files

Start by importing `scalaql`:

```scala mdoc
import scalaql._

// Docs classes
import scalaql.docs.Hogwarts._
import scalaql.docs.DocUtils._

// Imports for examples
import java.nio.file.Paths
```

## Basic write

In this example, we'll read an existing CSV file, process it and write the result into a new CSV file.  
Input file:

```scala mdoc
val studentsPath = Paths.get("docs/src/main/resources/students.csv")

printFile(studentsPath)
```

First, define an aggregation `Query`:

```scala mdoc
case class FacultyInfo(
  name: String, 
  avgAge: Double, 
  totalGrade: Double)

val aggregation: Query[From[Student], FacultyInfo] =
  select[Student]
    .groupBy(_.faculty)
    .aggregate((faculty, students) =>
      (
        students.avgBy(_.age.toDouble) &&
        students.sumBy(_.grade)
      ).map{ case (avgAge, totalGrade) => FacultyInfo(faculty, avgAge, totalGrade) }
    )
```

Then you could write the result into a CSV file as follows:

```scala mdoc

val outPath = Paths.get("docs/target/stats.csv")

aggregation
  .foreach(
    csv.write[FacultyInfo].file(outPath)
  )
  .run(
    from(
      csv.read[Student].file(studentsPath)
    )
  )
```

It will generate a CSV file with the following content:

```scala mdoc
printFile(outPath)
```

## Naming

You could also customize naming style for CSV header.  
By default, headers have the same names as case class fields.

Start with the following imports:

```scala mdoc:reset
import scalaql._
import scalaql.csv.CsvWriteConfig

// Docs classes
import scalaql.docs.Hogwarts._
import scalaql.docs.DocUtils._

// Imports for examples
import java.nio.file.Paths
```

With the same aggregation query:

```scala mdoc
case class FacultyInfo(
  name: String, 
  avgAge: Double, 
  totalGrade: Double)

val aggregation: Query[From[Student], FacultyInfo] =
  select[Student]
    .groupBy(_.faculty)
    .aggregate((faculty, students) =>
      (
        students.avgBy(_.age.toDouble) &&
        students.sumBy(_.grade)
      ).map{ case (avgAge, totalGrade) => FacultyInfo(faculty, avgAge, totalGrade) }
    )
```

This is how to produce a CSV file with snake_case headers:

```scala mdoc
val studentsPath = Paths.get("docs/src/main/resources/students.csv")
val outPathSnakeCase = Paths.get("docs/target/stats_snake_case.csv")

aggregation
  .foreach(
    csv
      .write[FacultyInfo]
      .option(Naming.SnakeCase)
      .file(outPathSnakeCase)
  )
  .run(
    from(
      csv.read[Student].file(studentsPath)
    )
  )
```

It will produce the following CSV file:

```scala mdoc
printFile(outPathSnakeCase)
```
