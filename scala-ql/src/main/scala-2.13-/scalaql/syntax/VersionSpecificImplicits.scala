package scalaql.syntax

import java.time.{LocalDate, LocalDateTime}

trait VersionSpecificImplicits {

  /** There is no `Ordering` for [[LocalDate]] in scala 2.12 */
  implicit val localDateOrdering: Ordering[LocalDate] = new Ordering[LocalDate] {
    override def compare(x: LocalDate, y: LocalDate): Int =
      x.compareTo(y)
  }

  /** There is no `Ordering` for [[LocalDateTime]] in scala 2.12 */
  implicit val localDateTimeOrdering: Ordering[LocalDateTime] = new Ordering[LocalDateTime] {
    override def compare(x: LocalDateTime, y: LocalDateTime): Int =
      x.compareTo(y)
  }
}
