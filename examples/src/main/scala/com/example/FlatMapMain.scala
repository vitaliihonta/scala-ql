package com.example

import scalaql._
import Hogwarts._

object FlatMapMain extends App {

  // it's cartesian, not join
  val query: Query[From[Student] with From[Faculty], (Student, Faculty)] = for {
    student <- select[Student].filter(_.age >= 18)
    faculty <- select[Faculty]
    if student.faculty == faculty.name
  } yield (student, faculty)

  println {
    query.toList
      .run(
        from(students) & from(faculties)
      )
      .mkString("\n")
  }
}
