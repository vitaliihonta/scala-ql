# Arrow composition

For clarity reasons, it's OK to split a single `Query` into multiple ones.

Start by importing:

```scala mdoc
import scalaql._

// Docs classes
import scalaql.docs.Hogwarts._
```

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
