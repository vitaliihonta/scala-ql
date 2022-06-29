# Aggregations

Start by importing:

```scala mdoc
import scalaql._

// Docs classes
import scalaql.docs.Hogwarts._
```

## Basic groupBy

You could define aggregations for your data as you do with plain `SQL`.
To evaluate such kind of aggregation:

```scala mdoc
case class FacultyInfo(
  name: String, 
  avgAge: Double, 
  totalGrade: Double, 
  specializations: List[String])
```

First, start with `groupBy`:

```scala mdoc
val grouped = select[Student].groupBy(_.faculty)
```

Then you could aggregate this query:

```scala mdoc
val facultyInfos = grouped
  .aggregate((faculty, students) =>
    (
      students.avgBy(_.age.toDouble) &&
      students.sumBy(_.grade) &&
      students.map(_.specialization).toList
    ).map { 
      case (avgAge, totalGrade, specializations) => 
        FacultyInfo(faculty, avgAge, totalGrade, specializations) 
    }
  )
```

`aggregate` function provides you the grouping key (in this case - `faculty`) and a special value to declare your
aggregations.

In this example:

- To calculate average age - use `students.avgBy(_.age.toDouble)`
- To get total grade - use `students.sumBy(_.grade)`
- To collect each specialization of the faculty - use `students.map(_.specialization).toList`

You could chain multiple aggregations by `&&` operator. It will automatically flatten the results into a single tuple.  
Then it's possible to `map` the aggregation result.

This is the result of running such query:

```scala mdoc
facultyInfos
  .show(truncate=false)
  .run(
    from(students)
  )
```

## Reports

`report` aggregation function is somehow similar to `groupBy` with multiple keys.  
The difference is that `report` allows you to do aggregations on intermediate results by using `combine` method.

First, start by defining report case class:

```scala mdoc
case class BySpec(specialization: String, avgGrade: Double)

case class ByAge(age: Int, grades: List[BySpec])

case class ByFaculty(faculty: String, grades: List[ByAge])
```

Then, you could define the aggreagtion as follows:  

```scala mdoc
val byFacultyReportQuery: Query[From[Student], ByFaculty] =
  select[Student]
    .groupBy(_.faculty)
    .aggregate { case (faculty, students) =>
      students
        .report(_.age, _.specialization) { (age, spec, students) =>
          // We know Dumbledore too well =)
          val additionalPoints = if (faculty == Gryffindor.name) 100 else 0
          
          students.avgBy(_.grade + additionalPoints)
            .map(BySpec(spec, _))
        }
        .combine((age, cascadeInfos) => cascadeInfos.toList.map(ByAge(age, _)))
        .map(byAge => ByFaculty(faculty, byAge))
    }
```

In this case, you define your aggregation as always. Notice the following:

- Usage of `report` aggregation function
- `report` accepts grouping keys (like `age` and `specialization`)
- First, you define aggregations per all grouping keys (both `age` and `specialization`)
- Then, using `combine`, you aggregate on `age` grouping key level
- Inside `combine` you'll use a deeper level aggregation result
- ... and so on until you combine all nested aggregations
  
Then run the query:

```scala mdoc
byFacultyReportQuery
  .show(truncate = false)
  .run(from(students))
```

## Available aggregation functions

- `toList` - collects records into a `List`
- `distinct` - collects distinct records into a `Set`
- `distinctBy` - equivalent of `listField.map(_.map(f)).distinct`
- `flatDistinctBy` - equivalent of `listField.flatMap(_.map(f)).distinct`
- `const` - return a constant value as an aggregation result
- `sum` - sum the field
- `sumBy` - sum the field by some other value
- `product` - multiplication of field values
- `productBy` - multiplication of field values by some other value
- `avg` - average value of the field
- `avgBy` - average value of the field by some other value
- `std` - standard deviation of the field
- `stdBy` - standard deviation of the field by some other value
- `count` - number of values for each given predicate holds
- `size` - total number of values
- `report(K1, K2, ...)` - report of arity N