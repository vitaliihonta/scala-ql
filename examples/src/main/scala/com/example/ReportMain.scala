package com.example

import scalaql.*
import com.example.Hogwarts.*

object ReportMain extends App {

  val query: Query[From[Student], ByFaculty] =
    select[Student]
      .groupBy(_.faculty)
      .aggregate { case (faculty, students) =>
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
      .map((ByFaculty.apply _).tupled)
      .sortBy(_.faculty)(desc)

  query
    .show(truncate = false)
    .run(from(students))

}
