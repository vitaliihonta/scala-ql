# Reading JSON files

<head>
  <meta charset="UTF-8" />
  <meta name="author" content="Vitalii Honta" />
  <meta name="description" content="Getting started with Scala QL JSON module. Reading JSON files" />
  <meta name="keywords" content="scala-ql, scala-ql-json, scala process json files, scala-ql read json" />
</head>

## Reading single file

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

Assume you have a JSON file like the following:

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
      json.read[Student].file(studentsPath)
    )
  )
```