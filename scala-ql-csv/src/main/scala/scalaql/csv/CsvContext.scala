package scalaql.csv

import scalaql.sources.Naming
import scalaql.sources.columnar.{CodecPath, TableApiContext, TableApiWriteContext}

case class CsvReadContext(location: CodecPath, naming: Naming) extends TableApiContext[CsvReadContext] {
  def getFieldName: String =
    naming(location.fieldLocation.name)

  def cannotDecodeError(cause: String): CsvDecoderException =
    new CsvDecoderException(
      s"Cannot decode cell at path `$location`: $cause"
    )

  override def enterField(name: String): CsvReadContext =
    copy(location = CodecPath.AtField(name, location))

  override def enterIndex(idx: Int): CsvReadContext =
    copy(location = CodecPath.AtIndex(idx, location.fieldLocation))
}

object CsvReadContext {
  def initial(naming: Naming): CsvReadContext = CsvReadContext(
    location = CodecPath.Root,
    naming = naming
  )
}

case class CsvWriteContext(location: CodecPath, headers: List[String], naming: Naming)
    extends TableApiWriteContext[CsvWriteContext] {
  def getFieldName: String =
    naming(location.fieldLocation.name)

  override def enterField(name: String): CsvWriteContext =
    copy(location = CodecPath.AtField(name, location))

  override def enterIndex(idx: Int): CsvWriteContext =
    copy(location = CodecPath.AtIndex(idx, location.fieldLocation))
}

object CsvWriteContext {
  def initial(headers: List[String], naming: Naming): CsvWriteContext = CsvWriteContext(
    location = CodecPath.Root,
    headers = headers,
    naming = naming
  )
}
