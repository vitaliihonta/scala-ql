package scalaql.csv

sealed trait CsvEntry

object CsvEntry {
  case class Row(row: Map[String, String]) extends CsvEntry
  case class Field(field: String)          extends CsvEntry
}
