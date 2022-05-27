package com.example

import scalaql._
import Hogwarts._

object ReportMain extends App {

  case class BySpec(specialization: String, avgGrade: Double)

  case class ByAge(age: Int, grades: List[BySpec])

  case class ByFaculty(faculty: String, grades: List[ByAge])

  val query: Query[From[Student], ByFaculty] =
    select[Student]
      .groupBy(_.faculty)
      .aggregate {
        case (faculty, students) =>
          students
            .report(_.age, _.specialization)((age, spec, students) =>
              if (faculty == Gryffindor.name) {
                students.avgBy(_.grade).map(_ + 100).map(BySpec(spec, _))
              } else if (age >= 18) {
                students.avgBy(_.grade).map(BySpec(spec, _))
              } else {
                students.const(BySpec(spec, avgGrade = 0))
              }
            )((age, cascadeInfos) => cascadeInfos.toList.map(ByAge(age, _)))
      }
      .map(ByFaculty.tupled)
      .sortBy(_.faculty)(desc)

  println {
    query.toList.run(from(students)).mkString("\n")
  }
}
