# Aliasing

<head>
  <meta charset="UTF-8" />
  <meta name="author" content="Vitalii Honta" />
  <meta name="description" content="Alias a query in Scala QL. How to distinguish data sources of the same type using aliases" />
  <meta name="keywords" content="scala-ql, scala-ql alias" />
</head>

Sometimes you may need to combine queries with the same data type, but possibly with different records.
Start by importing:

```scala mdoc
import scalaql._

// Docs classes
import scalaql.docs.Hogwarts._
```

Do distinguish those `Query` inputs, you could use `as` method to give `Query` input an alias:

```scala mdoc
val left = select[Student].as("left")

val right = select[Student].as("right")
```

Then you could use them both in one `Query`.  
For instance, let's find students peers:

```scala mdoc
case class Peer(who: String, age: Int, peer: Option[String])

val peers =
  left
    .leftJoin(right)
    .on(_.age == _.age)
    .map { case (left, rightOpt) =>
      Peer(
        who = left.name,
        age = left.age,
        peer = rightOpt.map(_.name)
      )
    }
```

To provide an aliased input, use the same `as` method on `from`:

```scala mdoc
val input = 
  from(students).as("left") &
    from(students).as("right")
```

Then run the `Query`:

```scala mdoc
peers
  .show(truncate = false)
  .run(input)
```

**NOTE**  
Scala 2.12 doesn't support literal types, so you should alias using traits:

```scala
trait left

trait right

val left = select[Student].as[left]

val right = select[Student].as[right]
```