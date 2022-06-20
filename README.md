[![Support ukraine](https://img.shields.io/static/v1?label=United24&message=Support%20Ukraine&color=lightgrey&link=https%3A%2F%2Fu24.gov.ua&logo=data%3Aimage%2Fpng%3Bbase64%2CiVBORw0KGgoAAAANSUhEUgAAASwAAADICAYAAABS39xVAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAANKSURBVHhe7dZBThRhFEbRnx1IgvtFiIoxbgemOHLAhAoJ1QyaBahroKxqE%2BMS6iZncPKSbwE3b4yr6W58en4Z148zwC5tjbqabrdgvZ59PS5nn2eAfVobtbbquAXrcBquJ4B9Wht1%2BrQEC9g9wQIyBAvIECwgQ7CADMECMgQLyBAsIEOwgAzBAjIEC8gQLCBDsIAMwQIyBAvIECwgQ7CADMECMgQLyBAsIEOwgAzBAjIEC8gQLCBDsIAMwQIyBAvIECwgQ7CADMECMgQLyBAsIEOwgAzBAjIEC8gQLCBDsIAMwQIyBAvIECwgQ7CADMECMgQLyBAsIEOwgAzBAjIEC8gQLCBDsIAMwQIyBAvIECwgQ7CADMECMgQLyBAsIEOwgAzBAjIEC8gQLCBDsIAMwQIyBAvIECwgQ7CADMECMgQLyBAsIEOwgAzBAjIEC8gQLCBDsIAMwQIyBAvIECwgQ7CADMECMgQLyBAsIEOwgAzBAjIEC8gQLCBDsIAMwQIyBAvIECwgQ7CADMECMgQLyBAsIEOwgAzBAjIEC8j4F6zL%2BTA%2BHpfxYR0A9mhr1OXzPC5u7g%2Fvv%2F1YLr58B9ilU6Nu7ufx6%2BH88Hs6X9YLsEtbo34%2BvJvH29M4LC9jWZ4Admpt1NqqNVjTGqz5bFkmgJ1aG%2FX2KFhAgWABGYIFZAgWkCFYQIZgARmCBWQIFpAhWECGYAEZggVkCBaQIVhAhmABGYIFZAgWkCFYQIZgARmCBWQIFpAhWECGYAEZggVkCBaQIVhAhmABGYIFZAgWkCFYQIZgARmCBWQIFpAhWECGYAEZggVkCBaQIVhAhmABGYIFZAgWkCFYQIZgARmCBWQIFpAhWECGYAEZggVkCBaQIVhAhmABGYIFZAgWkCFYQIZgARmCBWQIFpAhWECGYAEZggVkCBaQIVhAhmABGYIFZAgWkCFYQIZgARmCBWQIFpAhWECGYAEZggVkCBaQIVhAhmABGYIFZAgWkCFYQIZgARmCBWQIFpAhWECGYAEZggVkCBaQIVhAhmABGYIFZAgWkCFYQIZgARmCBWQIFpAhWECGYAEZggVk%2FBes1%2BX4dwDYpbVRa6uOW7Du3p7Hy1YvgF3aGjWN2z9qCgwkg1n6XwAAAABJRU5ErkJggg%3D%3D)](https://u24.gov.ua)
[![scala-ql Scala version support](https://index.scala-lang.org/vitaliihonta/scala-ql/scala-ql/latest-by-scala-version.svg?platform=jvm)](https://index.scala-lang.org/vitaliihonta/scala-ql/scala-ql)
![Build status](https://github.com/vitaliihonta/scala-ql/actions/workflows/publish.yaml/badge.svg)
[![codecov](https://codecov.io/gh/vitaliihonta/scala-ql/branch/main/graph/badge.svg?token=T8NBC4R360)](https://codecov.io/gh/vitaliihonta/scala-ql)

# ScalaQL

ScalaQL is simple statically typed (and lawful) query DSL for scala.

The library provides a composable Query - a description of your computations,  
which you can then apply to multiple sources and write into multiple sinks.

## Use cases

- Generating data reports
- Data exploration

## Install

```sbt
// Core
libraryDependencies += "com.github.vitaliihonta" %% "scala-ql" % "<VERSION>"

// CSV support
libraryDependencies += "com.github.vitaliihonta" %% "scala-ql-csv" % "<VERSION>"

// JSON support
libraryDependencies += "com.github.vitaliihonta" %% "scala-ql-json" % "<VERSION>"

// Excel support
libraryDependencies += "com.github.vitaliihonta" %% "scala-ql-excel" % "<VERSION>"
```

## Supported sources

- Any scala collection
- CSV/TSV file
- JSON file (single line / multiline)
- Excel

ScalaQL is also able to read from multiple files at once  
and walking directory (optionally by GLOB pattern)

## Supported sinks

- Any scala collection
- CSV/TSV file
- JSON file (single line / multiline)
- Excel

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

  val studentsPath = Paths.get("examples/src/main/resources/students.csv")
  val facultiesPath = Paths.get("examples/src/main/resources/faculties.json")
  val outPath = Paths.get("examples/out/faculty_stats.csv")

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
