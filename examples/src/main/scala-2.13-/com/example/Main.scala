package com.example

import scalaql.*
import com.example.Hogwarts.*

object Main extends App {
  case class FacultyInfo(name: String, avgAge: Double, totalGrade: Double, specializations: List[String])

  val query: Query[From[Student], FacultyInfo] =
    select[Student]
      .where(_.age >= 18)
      .groupBy(_.faculty)
      .aggregate((faculty, students) =>
        students.avgBy(_.age.toDouble) &&
          students.sumBy(_.grade) &&
          students.map(_.specialization).toList
      )
      .map(FacultyInfo.tupled)

  println {
    query.to[Vector].run(from(students))
  }
}
