# Window functions

<head>
  <meta charset="UTF-8" />
  <meta name="author" content="Vitalii Honta" />
  <meta name="description" content="Window functions in Scala QL. How to define a query with windows like in standard SQL" />
  <meta name="keywords" content="scala-ql, scala window function, scala-ql window function" />
</head>

Scala QL supports window functions the same way as SQL.

Start by importing:

```scala mdoc
import scalaql._

// Docs classes
import scalaql.docs.DocUtils._
import scalaql.docs.WindowData._

// Used in examples
import java.nio.file.Paths
```

Let's predefine the query input:

```scala mdoc
val ordersPath = Paths.get("docs/src/main/resources/_Order__202207031241.csv")
val orderDetailsPath = Paths.get("docs/src/main/resources/OrderDetail_202207031246.csv")

def ordersInput = from(
  csv
    .read[Order]
    .option(Naming.Capitalize)
    .file(ordersPath)
) 

def orderDetailsInput = from(
  csv
    .read[OrderDetail]
    .option(Naming.Capitalize)
    .file(orderDetailsPath)
)
```

Let's check the input data set.

Orders:

```scala mdoc
select[Order]
  .show(truncate = false)
  .run(ordersInput)
```

Order details:

```scala mdoc
select[OrderDetail]
  .show(truncate = false)
  .run(orderDetailsInput)
```

Using window functions in Scala QL looks pretty similar to just plain SQL.  
Let's assume you'd like to run the following SQL equivalent:

```sql
SELECT CustomerId,
	   OrderDate,
     UnitPrice,
     AVG(UnitPrice) OVER (PARTITION BY CustomerId ORDER BY OrderDate DESC) AS AvgUnitPrice
FROM "Order"
INNER JOIN OrderDetail ON [Order].Id = OrderDetail.OrderId
```

This is how it will look like using Scala QL:

```scala mdoc
val query = select[Order]
  .join(select[OrderDetail])
  .on(_.id == _.orderId)
  .map((OrderWithDetails.apply _).tupled)
  .window(
    _.avgBy(_.details.unitPrice)
  )
  .over(
    _.partitionBy(_.order.customerId)
      .orderBy(_.order.orderDate.desc)
  )
  .map { case (data, avgUnitPrice) =>
    OrderStats(
      customerId = data.order.customerId,
      orderDate = data.order.orderDate,
      unitPrice = data.details.unitPrice,
      avgUnitPrice = avgUnitPrice
    )
  }
```

Let's run the `Query`:

```scala mdoc
query
  .show(truncate = false)
  .run(ordersInput & orderDetailsInput)
```
