# Writing JSON files

<head>
  <meta charset="UTF-8" />
  <meta name="author" content="Vitalii Honta" />
  <meta name="description" content="Getting started with Scala QL JSON module. Writing JSON files" />
  <meta name="keywords" content="scala-ql, scala-ql-json, scala process json files, scala-ql write json into file" />
</head>

Start by importing `scalaql`:

```scala mdoc
import scalaql._

// Docs classes
import scalaql.docs.Hogwarts._
import scalaql.docs.DocUtils._

// Imports for examples
import java.nio.file.Paths
import io.circe.generic.auto._
```

## Basic write

In this example, we'll read an existing JSON file, process it and write the result into a new CSV file.  
Input file:

```scala mdoc
val studentsPath = Paths.get("docs/src/main/resources/students.json")

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

val outPath = Paths.get("docs/target/stats.json")

aggregation
  .foreach(
    json.write[FacultyInfo].file(outPath)
  )
  .run(
    from(
      json.read[Student].file(studentsPath)
    )
  )
```

It will generate a CSV file with the following content:

```scala mdoc
printFile(outPath)
```

## Single line

You could also customize the resulting JSON file format.  
By default, it writes each JSON document into a separate file line.

Alternatively, you could write them into a single array.

Start with the following imports:

```scala mdoc:reset
import scalaql._

// Docs classes
import scalaql.docs.Hogwarts._
import scalaql.docs.DocUtils._

// Imports for examples
import java.nio.file.Paths
import io.circe.generic.auto._
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

This is how to produce a JSON file with array:

```scala mdoc
val studentsPath = Paths.get("docs/src/main/resources/students.json")
val outPathArray = Paths.get("docs/target/stats_array.json")

aggregation
  .foreach(
    json
      .write[FacultyInfo]
      .option(multiline = false)
      .file(outPathArray)
  )
  .run(
    from(
      json.read[Student].file(studentsPath)
    )
  )
```

It will produce the following JSON file:

```scala mdoc
printFile(outPathArray)
```
