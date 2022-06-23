package com.example

import scalaql.*
import com.example.Hogwarts.*

object FlatMapMain extends App {

  // it's cartesian, not join
  val query: Query[From[Student] with From[Faculty], (Student, Faculty)] = for {
    student <- select[Student].where(_.age >= 18)
    faculty <- select[Faculty].where(_.name isIn ("Gryffindor", "Hufflepuff"))
    if student.faculty == faculty.name
  } yield (student, faculty)

  query
    .foreach { case (student, faculty) => println(s"student=$student faculty=$faculty") }
    .run(
      from(students) & from(faculties)
    )
}
