package com.example

import com.example.Hogwarts._
import scalaql._

object JoinMain extends App {

  case class StudentOfFaculty(student: Student, faculty: Faculty)

  val query: Query[From[Faculty] with From[Student], StudentOfFaculty] = select[Student]
    .join(select[Faculty])
    .on(_.faculty == _.name)
    .map(StudentOfFaculty.tupled)

  println {
    query.toList
      .run(from(faculties) & from(students))
      .mkString("\n")
  }
}
