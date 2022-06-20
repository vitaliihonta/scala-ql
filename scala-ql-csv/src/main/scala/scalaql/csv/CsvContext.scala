package scalaql.csv

import scalaql.sources.Naming

case class CsvContext(path: List[String], naming: Naming) {
  def getFieldName: String =
    naming(path.head)

  def cannotDecodeError(path: List[String], cause: String): IllegalArgumentException = {
    val pathStr = path.reverse.map(n => s"`$n`").mkString(".")
    new IllegalArgumentException(
      s"Cannot decode cell at path $pathStr: $cause"
    )
  }
}
