package com.example

import scalaql.*
import com.example.Hogwarts.*

object AndThenMain extends App {

  case class FacultyInfo(faculty: String, avgAge: Double, avgGrade: Double, specs: Set[String])

  case class SpecializationInfo(faculty: String, specialization: String)

  val query1: Query[From[Student], FacultyInfo] = select[Student]
    .groupBy(_.faculty)
    .aggregate((_, student) =>
      student.avgBy(_.age.toDouble) &&
        student.avgBy(_.grade) &&
        student.map(_.specialization).distinct
    )
    .map((FacultyInfo.apply _).tupled)

  val query2: Query[From[FacultyInfo], SpecializationInfo] = select[FacultyInfo]
    .mapConcat(facultyInfo => facultyInfo.specs.map(facultyInfo.faculty -> _))
    .map((SpecializationInfo.apply _).tupled)

  val query: Query[From[Student], SpecializationInfo] = query1 >>> query2

  println {
    query.toList.run(from(students))
  }
}
