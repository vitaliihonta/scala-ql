# CSV Overview

<head>
  <meta charset="UTF-8" />
  <meta name="author" content="Vitalii Honta" />
  <meta name="description" content="Getting started with Scala QL CSV module. Processing CSV files" />
  <meta name="keywords" content="scala-ql, scala-ql-csv, scala process csv files, scala-ql-csv getting started" />
</head>

## Installation

To get started with CSV module, add the following dependency:

```scala
libraryDependencies += "@ORGANIZATION@" %% "scala-ql-csv" % "@VERSION@"
```

## Supported operations

- Reading single CSV files
- Reading multiple files
- Reading from directory (optionally with GLOB pattern)
- Writing query result to a CSV file
- Naming (snake_case, CamelCase, kebab-case, etc.)