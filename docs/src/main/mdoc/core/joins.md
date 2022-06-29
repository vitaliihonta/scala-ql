# Joins

Start by importing:

```scala mdoc
import scalaql._

// Docs classes
import scalaql.docs.Hogwarts._
```

## Inner join

Scala QL allows you to join multiple queries based on condition.

This could be done as follows:

```scala mdoc

val joinQuery = select[Student]
  .join(select[Faculty])
  .on(_.faculty == _.name)
```

For convenience, we'll wrap the join result into something more meaningful:

```scala mdoc
case class StudentAndFaculty(student: Student, faculty: Faculty)

val studentsAndFaculties = joinQuery.map((StudentAndFaculty.apply _).tupled)
```

It will produce the following result:

```scala mdoc
studentsAndFaculties
  .show(truncate = false)
  .run(
    from(students) & from(faculties)
  )
```

## Note on join semantics
Scala QL follows the SQL standard in case of joins.  
It means that if the `Query` input has duplicates, join will produce a result with duplicates.  

For instance, with the following input:
```scala mdoc
val duplicatedStudents = students ++ students
```

You'll get a result with duplicates:

```scala mdoc
studentsAndFaculties
  .show(truncate = false)
  .run(
    from(duplicatedStudents) & from(faculties)
  )
```

To avoid duplicates, you may use `deduplicate` method:

```scala mdoc
val deduplicatedQuery = studentsAndFaculties.deduplicate
```

This will produce a result without duplicates:

```scala mdoc
deduplicatedQuery
  .show(truncate = false)
  .run(
    from(duplicatedStudents) & from(faculties)
  )
```

If you need to deduplicate by a specific field, you may use `deduplicateBy` method:

```scala mdoc
val deduplicatedByQuery = studentsAndFaculties.deduplicateBy(_.student.name)
```

This will produce a result without duplicates:

```scala mdoc
deduplicatedByQuery
  .show(truncate = false)
  .run(
    from(duplicatedStudents) & from(faculties)
  )
```