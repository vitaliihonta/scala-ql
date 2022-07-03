# Overview

## Installation

```scala
// Core
libraryDependencies += "@ORGANIZATION@" %% "scala-ql" % "@VERSION@"

// CSV support
libraryDependencies += "@ORGANIZATION@" %% "scala-ql-csv" % "@VERSION@"

// JSON support
libraryDependencies += "@ORGANIZATION@" %% "scala-ql-json" % "@VERSION@"

// Excel support
libraryDependencies += "@ORGANIZATION@" %% "scala-ql-excel" % "@VERSION@"

// HTML support
libraryDependencies += "@ORGANIZATION@" %% "scala-ql-html" % "@VERSION@"
```

## Use cases

- Generating data reports
- Data exploration

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
- HTML

## Supported operations

- Functional composition (map, flatMap, andThen)
- Filtering (where, collect)
- Sub query filtering
- Group by / aggregate (sum, count, product, avg)
- Joins (inner, left, cross)
- ordering
- distinct
- union
- reports
