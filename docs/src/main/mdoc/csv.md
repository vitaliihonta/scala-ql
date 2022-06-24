# CSV

To get started with CSV module, add the following dependency:

```scala
libraryDependencies += "@ORGANIZATION@" %% "scala-ql-csv" % "@VERSION@"
```

Start by importing `scalaql`:

```scala mdoc
import scalaql._

// Docs classes
import scalaql.docs.Hogwarts._
import scalaql.docs.DocUtils._

// Imports for examples
import java.nio.file.Paths
```

Assume you have a CSV file like the following:

```scala mdoc
val studentsPath = Paths.get("docs/src/main/resources/students.csv")

printFile(studentsPath)
```

First, you defined a `Query` as usual:

```scala mdoc
val query =
  select[Student]
    .where(_.age >= 18)
```

Then you could specify to run the `Query` on a CSV file instead of a Scala collection:

```scala mdoc
query
  .show(truncate=false)
  .run(
    from(
      csv.read.file[Student](studentsPath)
    )
  )
```

You could aggregate through CSV as well.  
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
      students.avgBy(_.age.toDouble) &&
        students.sumBy(_.grade)
    )
    .map(FacultyInfo.tupled)
```

Then you could write it to a CSV file as follows:

```scala mdoc

val outPath = Paths.get("docs/target/stats.csv")

aggregation
  .foreach(
    csv.write.file(outPath)
  )
  .run(
    from(
      csv.read.file[Student](studentsPath)
    )
  )
```

It will generate a CSV file with the following content:

```scala mdoc
printFile(outPath)
```

You could also customize naming style for CSV header.  
By default, headers have the same names as case class fields.   
For instance, the following will produce snake_case headers:

```scala mdoc
import scalaql.csv.CsvWriteConfig
import scalaql.sources.Naming

val outPathSnakeCase = Paths.get("docs/target/stats_snake_case.csv")

def writeSnakeCase() = {
  implicit val csvConfig: CsvWriteConfig = CsvWriteConfig.default.copy(naming = Naming.SnakeCase)

  aggregation
    .foreach(
      csv.write.file(outPathSnakeCase)
    )
    .run(
      from(
        csv.read.file[Student](studentsPath)
      )
    )
} 
```

It will produce the following CSV file:

```scala mdoc
writeSnakeCase()

printFile(outPathSnakeCase)
```

CSV module also provides support for nested case classes.  
It will automatically flatten headers and rows when writing to a file.  

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
      students.avgBy(_.age.toDouble) &&
        students.sumBy(_.grade)
    )
    .map { case (faculty, avgAge, totalGrade) => 
       FacultyInfoNested(
         FacultyDescription(faculty),
         FacultyInfoStats(avgAge, totalGrade)
       )
    }
```

It will produce the following CSV file:

```scala mdoc

val nestedPath = Paths.get("docs/target/stats_nested.csv")

nestedAggregation
  .foreach(
    csv.write.file(nestedPath)
  )
  .run(
    from(
      csv.read.file[Student](studentsPath)
    )
  )
  
printFile(nestedPath)
```
