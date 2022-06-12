![Build status](https://github.com/vitaliihonta/scala-ql/actions/workflows/publish.yaml/badge.svg)
[![scala-ql Scala version support](https://index.scala-lang.org/vitaliihonta/scala-ql/scala-ql/latest.svg](https://index.scala-lang.org/vitaliihonta/scala-ql/scala-ql)

# ScalaQL

ScalaQL is simple statically typed (and lawful) query DSL for scala.

The library provides a composable Query - a description of your computations,  
which you can then apply to multiple sources and write into multiple sinks.

## Use cases

- Generating data reports
- Data exploration

## Supported sources/sinks

- Any scala collection
- CSV/TSV file
- JSON file (single line / multiline)
- [TBD] parquet

## Supported operations

- Functional composition (map, flatMap, andThen)
- Filtering (where, collect)
- Sub query filtering
- Group by / aggregate (sum, count, product, avg)
- Joins (inner, left, cross)
- sorting
- distinct
- union
- reports (see [example](./examples/src/main/scala/com/example/ReportMain.scala))

## Example

More examples could be found [here](./examples/src/main/)

```scala
package com.example

import scalaql._
import com.example.Hogwarts._
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

object FilesExample extends App {

  val query: Query[From[Student] with From[Faculty], FacultyStats] =
    select[Student]
      .where(_.age >= 18)
      .join(select[Faculty])
      .on(_.faculty == _.name)
      .groupBy { case (_, faculty) => faculty.name }
      .aggregate((_, studentsWithFaculties) => studentsWithFaculties.avgBy { case (student, _) => student.grade })
      .map((FacultyStats.apply _).tupled)

  val studentsPath  = Paths.get("examples/src/main/resources/students.csv")
  val facultiesPath = Paths.get("examples/src/main/resources/faculties.json")
  val outPath       = Paths.get("examples/out/faculty_stats.csv")

  query
    .foreach(
      csv.write.file[FacultyStats](outPath)
    )
    .run(
      from(
        csv.read.file[Student](studentsPath)
      ) & from(
        json.read.file[Faculty](facultiesPath)
      )
    )
}
```
