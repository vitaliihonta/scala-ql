package scalaql.describe

import java.time.{LocalDate, LocalDateTime}

trait AdditionalDescribeImplicits {

  private implicit def localDateOrdering = new Ordering[LocalDate] {
    override def compare(x: LocalDate, y: LocalDate): Int = x.compareTo(y)
  }

  private implicit def localDateTimeOrdering = new Ordering[LocalDateTime] {
    override def compare(x: LocalDateTime, y: LocalDateTime): Int = x.compareTo(y)
  }

  implicit val describeLocalDate: Describe[LocalDate]         = new DescribeOrdered[LocalDate]
  implicit val describeLocalDateTime: Describe[LocalDateTime] = new DescribeOrdered[LocalDateTime]
}
