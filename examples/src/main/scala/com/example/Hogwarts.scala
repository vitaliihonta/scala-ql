package com.example

object Hogwarts {

  case class Student(
    name:           String,
    age:            Int,
    faculty:        String,
    grade:          Double,
    specialization: String)

  case class Faculty(name: String, founder: String, description: String)

  val Gryffindor: Faculty = Faculty(
    name = "Gryffindor",
    founder = "Godric Gryffindor",
    description = "100 points by default"
  )

  val Slytherin: Faculty = Faculty(
    name = "Slytherin",
    founder = "Salazar Slytherin",
    description = "The bad guys"
  )

  val Hufflepuff: Faculty = Faculty(
    name = "Hufflepuff",
    founder = "Helga Hufflepuff",
    description = "Cedric studied here"
  )

  val Harry: Student = Student(
    name = "Harry",
    age = 19,
    faculty = Gryffindor.name,
    grade = 85.1,
    specialization = "learning"
  )

  val Ron: Student = Student(
    name = "Ron",
    age = 18,
    faculty = Gryffindor.name,
    grade = 66.2,
    specialization = "eating"
  )

  val Hermione: Student = Student(
    name = "Hermione",
    age = 18,
    faculty = Gryffindor.name,
    grade = 99.6,
    specialization = "learning"
  )

  val Draco: Student = Student(
    name = "Draco",
    age = 18,
    faculty = Slytherin.name,
    grade = 85.1,
    specialization = "trolling"
  )

  val Cedric: Student = Student(
    name = "Cedric",
    age = 17,
    faculty = Hufflepuff.name,
    grade = 90.1,
    specialization = "young dying"
  )

  val students: List[Student] = List(Harry, Ron, Hermione, Draco, Cedric)

  val faculties: List[Faculty] = List(Gryffindor, Slytherin, Hufflepuff)
}
