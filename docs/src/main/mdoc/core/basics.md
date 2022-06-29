# Basics

## Query

To use `scalaql` features, you should start by importing:

```scala mdoc
import scalaql._

// Docs classes
import scalaql.docs.Hogwarts._
```

The basic abstraction you'll use is a `Query`.
It's a description of computations you want to perform on your data.

It doesn't evaluate until you explicitly run it:

```scala mdoc
select[Student]
```

Then you could add basic filtering. For instance:

```scala mdoc
val filtered = select[Student]
  .where(_.age >= 18)
```

To run the query and collect the results into a `List`, there is a `run` method, which could be used as follows:

```scala mdoc
filtered.toList.run(from(students))
```

Alternatively, you could also print them to the console via `show` method.  
It would render your data as a table:

```scala mdoc
filtered.show(truncate=false).run(from(students))
```

## Data exploration

Assume you'd like to explore your data.  
You could do this simply by calling `describe` method on a `Query`:

```scala mdoc
select[Student]
  .describe()
  .show(truncate=false)
  .run(
    from(students)
  )
```

## Using for comprehension

It's allowed to use the `Query` inside a `for` comprehension.

For instance:

```scala mdoc
  // it's cartesian product, not a join
val flatMapQuery: Query[From[Student] with From[Faculty], (Student, Faculty)] = 
  for {
    student <- select[Student]
    if student.age >= 18
    faculty <- select[Faculty]
    if faculty.name isIn ("Gryffindor", "Hufflepuff")
    if student.faculty == faculty.name
  } yield (student, faculty)
```

**NOTE**  
When combining multiple `select` expressions, the type use pass as `Query` input will be automatically captured in
the `Query` type signature.  
In this case, the resulting `Query` input type will become `From[Student] with From[Faculty]`.  
This basically means that query expects both `Student` and `Faculty` data sources to be provided.

To run such `Query` with multiple inputs, you could use `&` operator on `from`:

```scala mdoc
val input = from(students) & from(faculties)
```

```scala mdoc
flatMapQuery
  .show(truncate = false)
  .run(input)
```
