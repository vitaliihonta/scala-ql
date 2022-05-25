package com.example

import scalaql._
import Hogwarts._

object SubSelectMain extends App {

  def hasAdults(faculty: Faculty) =
    select[Student]
      .filter(_.faculty == faculty.name)
      .exists(_.age >= 18)

  val query = select[Faculty].filterM(hasAdults)

  println {
    query.toList
      .run(
        from(students) & from(faculties)
      )
      .mkString("\n")
  }
}
