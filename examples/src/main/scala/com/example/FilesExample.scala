package com.example

import scalaql.*
import com.example.Hogwarts.*
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

object FilesExample extends App {

  val query: Query[From[Student] & From[Faculty], FacultyStats] =
    select[Student]
      .where(_.age >= 18)
      .join(select[Faculty])
      .on(_.faculty == _.name)
      .groupBy { case (_, faculty) => faculty.name }
      .aggregate((_, studentsWithFaculties) => studentsWithFaculties.avgBy { case (student, _) => student.grade })
      .map((FacultyStats.apply _).tupled)

  val studentsPath  = Paths.get("examples/src/main/resources/students.csv")
  val facultiesPath = Paths.get("examples/src/main/resources/faculties.json")
  val outPath       = Paths.get("examples/out/faculty_stats.csv")

  query
    .foreach(
      csv.write.file[FacultyStats](outPath)
    )
    .run(
      from(
        csv.read.file[Student](studentsPath)
      ) & from(
        json.read.file[Faculty](facultiesPath)
      )
    )
}
