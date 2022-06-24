# JSON

To get started with JSON module, add the following dependency:

```scala
libraryDependencies += "@ORGANIZATION@" %% "scala-ql-json" % "@VERSION@"
```

JSON module depends on [circe](https://circe.github.io/circe/) for parsing JSON.

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

Assume you have a json file like the following:

```scala mdoc
val studentsPath = Paths.get("docs/src/main/resources/students.json")

printFile(studentsPath)
```

First, you defined a `Query` as usual:

```scala mdoc
val query =
  select[Student]
    .where(_.age >= 18)
```

Then you could specify to run the `Query` on a json file instead of a Scala collection:

```scala mdoc
query
  .show(truncate=false)
  .run(
    from(
      json.read.file[Student](studentsPath)
    )
  )
```

You could aggregate through JSON as well.  
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

Then you could write it to a JSON file as follows:

```scala mdoc

val outPath = Paths.get("docs/target/stats.json")

aggregation
  .foreach(
    json.write.file(outPath)
  )
  .run(
    from(
      json.read.file[Student](studentsPath)
    )
  )
```

It will generate a JSON file with the following content:

```scala mdoc
printFile(outPath)
```

You could also customize format of your JSON file.  
By default, it generates object-per-row (aka json-row format).
Alternatively, you could both parse and write single-array JSON files.
For instance, the following will produce single-array JSON file:

```scala mdoc
import scalaql.json.JsonWriteConfig
import scalaql.sources.Naming

val outSingleLine = Paths.get("docs/target/stats_single_line.json")

def writeSingleLine() = {
  implicit val jsonConfig: JsonWriteConfig = JsonWriteConfig.default.copy(multiline = false)

  aggregation
    .foreach(
      json.write.file(outSingleLine)
    )
    .run(
      from(
        json.read.file[Student](studentsPath)
      )
    )
} 
```

It will produce the following json file:

```scala mdoc
writeSingleLine()

printFile(outSingleLine)
```