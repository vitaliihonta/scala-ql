# Sub selects

A typical case you may require is running a `Query` which depends on another `Query` result.  

Start by importing:

```scala mdoc
import scalaql._

// Docs classes
import scalaql.docs.Hogwarts._
```

Assume you'd like to find faculties that have adult students.  
This could be done via `exists` method:

```scala mdoc
def hasAdults(faculty: Faculty) =
  select[Student]
    .where(_.faculty == faculty.name)
    .exists(_.age >= 18)
```

You could apply this function (which uses a `Query`) on another `Query` via `whereSubQuery`:

```scala mdoc
val subSelectQuery = 
  select[Faculty]
    .whereSubQuery(hasAdults)
```

Then run it:

```scala mdoc
subSelectQuery
  .show(truncate = false)
  .run(
    from(students) & from(faculties)
  )
```