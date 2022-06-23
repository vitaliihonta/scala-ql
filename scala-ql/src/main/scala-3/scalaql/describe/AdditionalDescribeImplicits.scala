package scalaql.describe

import java.time.{LocalDate, LocalDateTime}

trait AdditionalDescribeImplicits {
  implicit val describeLocalDate: Describe[LocalDate]         = new DescribeOrdered[LocalDate]
  implicit val describeLocalDateTime: Describe[LocalDateTime] = new DescribeOrdered[LocalDateTime]
}
