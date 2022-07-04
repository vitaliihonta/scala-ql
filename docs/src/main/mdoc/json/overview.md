# JSON overview

<head>
  <meta charset="UTF-8" />
  <meta name="author" content="Vitalii Honta" />
  <meta name="description" content="Getting started with Scala QL JSON module. Processing JSON files" />
  <meta name="keywords" content="scala-ql, scala-ql-json, scala process json files, scala-ql-json getting started" />
</head>

## Installation

To get started with JSON module, add the following dependency:

```scala
libraryDependencies += "@ORGANIZATION@" %% "scala-ql-json" % "@VERSION@"
```

JSON module depends on [circe](https://circe.github.io/circe/) for parsing JSON.

## Supported operations

- Reading single JSON files
- Reading multiple files
- Reading from directory (optionally with GLOB pattern)
- Writing query result to a JSON file (single and multiline)