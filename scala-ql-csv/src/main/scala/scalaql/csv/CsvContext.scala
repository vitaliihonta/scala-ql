package scalaql.csv

import scalaql.sources.Naming

case class CsvContext(path: List[String], naming: Naming) {
  def getFieldName: String =
    naming(path.head)

  def pathStr: String =
    if (path.isEmpty) "root"
    else path.reverse.map(n => s"`$n`").mkString(".")

  def cannotDecodeError(cause: String): CsvDecoderException =
    new CsvDecoderException(
      s"Cannot decode cell at path $pathStr: $cause"
    )
}

object CsvContext {
  def initial(naming: Naming): CsvContext = CsvContext(Nil, naming)
}
