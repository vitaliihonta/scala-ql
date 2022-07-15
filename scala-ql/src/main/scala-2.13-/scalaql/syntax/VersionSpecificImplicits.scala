package scalaql.syntax

import java.time.{LocalDate, LocalDateTime}

trait VersionSpecificImplicits {
  implicit val localDateOrdering: Ordering[LocalDate] = new Ordering[LocalDate] {
    override def compare(x: LocalDate, y: LocalDate): Int =
      x.compareTo(y)
  }

  implicit val localDateTimeOrdering: Ordering[LocalDateTime] = new Ordering[LocalDateTime] {
    override def compare(x: LocalDateTime, y: LocalDateTime): Int =
      x.compareTo(y)
  }
}
