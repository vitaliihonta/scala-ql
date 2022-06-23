package com.example

import scalaql.*
import com.example.Hogwarts.*

object DescribeMain extends App {
  select[Student]
    .describe(unique = true)
    .show(truncate = false)
    .run(
      from(students)
    )
}
