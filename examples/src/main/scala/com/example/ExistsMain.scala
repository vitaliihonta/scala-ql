package com.example

import scalaql.*
import com.example.Hogwarts.*

object ExistsMain extends App {

  val query = for {
    studentGradesByFaculty <-
      select[Student]
        .where(_.age >= 18)
        .groupBy(_.faculty)
        .aggregate { (faculty, students) =>
          (
            students.avgBy(_.age.toDouble) &&
              students.sumBy(_.grade)
          ).map(faculty -> _)
        }
        .toMapBy { case (faculty, _, _) => faculty }
    hasCoolAdults <- select[Faculty].exists { faculty =>
                       val (_, avgAge, avgGrade) = studentGradesByFaculty(faculty.name)
                       avgAge > 18 && avgGrade > 95
                     }
  } yield hasCoolAdults

  println {
    query
      .run(
        from(students) & from(faculties)
      )
  }
}
