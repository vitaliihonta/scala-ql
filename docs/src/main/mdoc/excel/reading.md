# Reading files

## Reading document

Start by importing `scalaql`:

```scala mdoc
import scalaql._
import scalaql.sources.Naming
import scalaql.excel.{ExcelReadConfig, CellResolutionStrategy}

// Docs classes
import scalaql.docs.ExcelData._

// Imports for examples
import java.nio.file.Paths
import java.time.LocalDate
```

Assume you have an Excel file like the following:

```scala mdoc
val ordersPath = Paths.get("docs/src/main/resources/orders_data.xlsx")
```

Which looks like:  

![Input excel document](excel_input_file.png)

This specific Excel file has UpperCased headers.
To read it, you should first specify `ExcelReadConfig`:

```scala mdoc
implicit val excelReadConfig: ExcelReadConfig = ExcelReadConfig.default.copy(
  naming = Naming.UpperCase,
  cellResolutionStrategy = CellResolutionStrategy.NameBased,
)
```

First, you defined a `Query` as usual:

```scala mdoc
val query = select[OrderInfo]
```

You can quickly view the file content:

```scala mdoc
query
  .show(truncate=false)
  .run(
    from(
      excel.read.file[OrderInfo](ordersPath)
    )
  )
```