# Ordering

<head>
  <meta charset="UTF-8" />
  <meta name="author" content="Vitalii Honta" />
  <meta name="description" content="Query ordering in Scala QL. How to easily order data by multiple fields" />
  <meta name="keywords" content="scala-ql, scala-ql ordering, scala-ql order by" />
</head>

Start by importing:

```scala mdoc
import scalaql._

// Docs classes
import scalaql.docs.Hogwarts._
```

Usually, sorting scala collections is simple, but it looks awkwardly when it comes to descending ordering.  
To sort this by age in descending order, you should do the following:

```scala mdoc
students.sortBy(_.age)(Ordering[Int].reverse)
```

Additionally sorting by name in ascending order looks even more awkwardly:

```scala mdoc
students.sortBy(s => (s.age, s.name))(Ordering.Tuple2(Ordering[Int].reverse, Ordering[String]))
```

Scala QL simplifies this a lot! Just take a look:  

```scala mdoc
val query = select[Student].orderBy(_.age.desc, _.name)

query
  .show(truncate = false)
  .run(from(students))
```