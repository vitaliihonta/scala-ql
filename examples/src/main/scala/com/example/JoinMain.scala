package com.example

import scalaql.*
import com.example.Hogwarts.*

object JoinMain extends App {

  case class StudentOfFaculty(student: Student, faculty: Faculty)

  val query: Query[From[Faculty] with From[Student], StudentOfFaculty] = select[Student]
    .join(select[Faculty])
    .on(_.faculty == _.name)
    .map((StudentOfFaculty.apply _).tupled)

  println {
    query.toList
      .run(from(faculties) & from(students))
      .mkString("\n")
  }
}
