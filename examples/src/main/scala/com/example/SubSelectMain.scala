package com.example

import scalaql.*
import com.example.Hogwarts.*

object SubSelectMain extends App {

  def hasAdults(faculty: Faculty) =
    select[Student]
      .where(_.faculty == faculty.name)
      .exists(_.age >= 18)

  val query = select[Faculty].whereSubQuery(hasAdults)

  println {
    query.toList
      .run(
        from(students) & from(faculties)
      )
      .mkString("\n")
  }
}
