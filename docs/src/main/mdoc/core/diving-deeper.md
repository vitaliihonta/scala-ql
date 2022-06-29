# Diving deeper

Start by importing:

```scala mdoc
import scalaql._

// Docs classes
import scalaql.docs.Hogwarts._
```

## Aliasing

Sometimes you may need to combine queries with the same data type, but possibly with different records.  
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

## Use another Query's result as an input

For clarity reasons, it's OK to split a single `Query` into multiple ones.

Assume you'd like to calculate some statistics for adult students:

```scala mdoc

case class AdultStudent(name: String, faculty: String, age: Int, grade: Double)

case class AdultStudentsStats(
  faculty:  String,
  avgAge:   Double,
  avgGrade: Double)
```

First, you define a `Query` to keep only adults:

```scala mdoc
val adultStudents = select[Student]
  .where(_.age >= 18)
  .map(student => 
    AdultStudent(
      name = student.name, 
      faculty = student.faculty, 
      age = student.age, 
      grade = student.grade
    )
  )
```

Then, you define an aggregation `Query`:

```scala mdoc
val adultStats = select[AdultStudent]
  .groupBy(_.faculty)
  .aggregate((faculty, adults) => 
    (
      adults.avgBy(_.age.toDouble) &&
      adults.sumBy(_.grade)
    ).map { case (avgAge, avgGrade) => AdultStudentsStats(faculty, avgAge, avgGrade) }
  )
```

It's pretty easy to combine those queries using `>>>` operator:

```scala mdoc
val resultQuery = adultStudents >>> adultStats
```

Then you could run this query as usual:

```scala mdoc
resultQuery
  .show(truncate = false)
  .run(from(students))
```

## Sub select & exists
TBD