# Reading files

## Reading single file

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
val students =
  select[Student]
    .where(_.age >= 18)
```

Then you could specify to run the `Query` on a CSV file instead of a Scala collection:

```scala mdoc
students
  .show(truncate=false)
  .run(
    from(
      csv.read[Student].file(studentsPath)
    )
  )
```

## Reading from directory

You could also read multiple CSV files from an arbitrary nested directories using GLOB pattern.

Start with the following imports:
```scala mdoc:reset
import scalaql._
import scalaql.sources.Naming

// Docs classes
import scalaql.docs.CsvData._
import scalaql.docs.DocUtils._

// Imports for examples
import java.nio.file.Paths
```

Then define a query:

```scala mdoc
val enterpriseSurvey =
  select[EnterpriseSurvey]
    .where(_.year >= 2015)
```

And then read the files:

```scala mdoc
val dir = Paths.get("docs/src/main/resources/annual-enterprise-survey-2020/")

enterpriseSurvey
  .show(truncate=false)
  .run(
    from(
      csv
        .read[EnterpriseSurvey]
        // In this case it's required to provide correct naming. 
        .option(Naming.SnakeCase)
        .directory(dir, globPattern = "**/*.csv")
    )
  )
```