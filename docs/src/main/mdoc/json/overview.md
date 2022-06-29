# JSON overview

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