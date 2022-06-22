package scalaql.html

import scalaql.sources.columnar.{CodecPath, TableApiWriteContext}
import scalatags.Text.{Modifier, TypedTag}

case class HtmlTableEncoderContext(
  location:    CodecPath,
  headers:     List[String],
  trTag:       TypedTag[String],
  thTag:       TypedTag[String],
  tdTag:       TypedTag[String],
  fieldStyles: String => List[Modifier])
    extends TableApiWriteContext[HtmlTableEncoderContext] { self =>

  override def enterField(name: String): HtmlTableEncoderContext =
    copy(location = CodecPath.AtField(name, self.location))

  override def enterIndex(idx: Int): HtmlTableEncoderContext =
    copy(location = CodecPath.AtIndex(idx, self.fieldLocation))

  def getFieldStyles: List[Modifier] =
    if (location.isField) fieldStyles(fieldLocation.name)
    else Nil
}

object HtmlTableEncoderContext {
  def initial(
    headers:     List[String],
    fieldStyles: String => List[Modifier],
    trTag:       TypedTag[String],
    thTag:       TypedTag[String],
    tdTag:       TypedTag[String]
  ): HtmlTableEncoderContext =
    HtmlTableEncoderContext(
      location = CodecPath.Root,
      headers = headers,
      fieldStyles = fieldStyles,
      trTag = trTag,
      thTag = thTag,
      tdTag = tdTag
    )
}
