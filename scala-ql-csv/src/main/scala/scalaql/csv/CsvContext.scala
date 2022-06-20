package scalaql.csv

import scalaql.sources.Naming

case class CsvContext(path: List[String], naming: Naming) {
  def getFieldName: String =
    naming(path.head)

  def cannotDecodeError(cause: String): CsvDecoderException = {
    val pathStr = path.reverse.map(n => s"`$n`").mkString(".")
    new CsvDecoderException(
      s"Cannot decode cell at path $pathStr: $cause"
    )
  }
}
