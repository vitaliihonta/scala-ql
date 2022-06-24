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
```

Then you could run you query on data from CSV file:

```scala mdoc
import java.nio.file.Paths

val studentsPath = Paths.get("docs/src/main/resources/students.csv")
  
val query =
  select[Student]
    .where(_.age >= 18)

query
  .show(truncate=false)
  .run(
    from(
      csv.read.file[Student](studentsPath)
    )
  )
```