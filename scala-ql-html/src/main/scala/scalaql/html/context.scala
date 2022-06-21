package scalaql.html

import scalatags.Text.{Modifier, TypedTag}
import scala.annotation.tailrec

sealed trait CodecPath {
  def isRoot: Boolean
  def isIndex: Boolean
  def isField: Boolean
  def parent: CodecPath

  protected[scalaql] def fieldLocation: CodecPath.AtField =
    sys.error("Library error, please fill a bug ticket")

  override final def toString: String = {
    @tailrec
    def go(remaining: CodecPath, acc: List[String]): List[String] =
      remaining match {
        case CodecPath.Root => acc
        case CodecPath.AtField(name, parent) =>
          go(parent, name :: acc)
        case CodecPath.AtIndex(idx, parent) =>
          go(parent, s"[$idx]" :: acc)
      }

    go(this, acc = Nil).mkString(".")
  }
}

object CodecPath {
  case object Root extends CodecPath {
    override val isRoot            = true
    override val isIndex           = false
    override val isField           = false
    override val parent: CodecPath = this
  }
  case class AtField(name: String, parent: CodecPath) extends CodecPath {
    override val isRoot  = false
    override val isIndex = false
    override val isField = true

    override val fieldLocation: AtField = this
  }
  case class AtIndex(idx: Int, parent: CodecPath.AtField) extends CodecPath {
    override val isRoot  = false
    override val isIndex = true
    override val isField = false
  }
}

case class HtmlTableEncoderContext(
  location:    CodecPath,
  headers:     List[String],
  trTag:       TypedTag[String],
  thTag:       TypedTag[String],
  tdTag:       TypedTag[String],
  fieldStyles: String => List[Modifier]) { self =>

  def enterField(name: String): HtmlTableEncoderContext =
    copy(location = CodecPath.AtField(name, self.location))

  def enterIndex(idx: Int): HtmlTableEncoderContext =
    copy(location = CodecPath.AtIndex(idx, self.fieldLocation))

  private[html] def fieldLocation: CodecPath.AtField =
    location.fieldLocation

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
