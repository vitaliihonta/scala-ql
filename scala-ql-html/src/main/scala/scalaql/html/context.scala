package scalaql.html

import scalatags.Text.Modifier
import scala.annotation.tailrec

sealed trait CodecPath {
  def isRoot: Boolean
  def isIndex: Boolean
  def isField: Boolean
  def parent: CodecPath

  override def toString: String = CodecPath.render(this)
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
  }
  case class AtIndex(idx: Int, parent: CodecPath.AtField) extends CodecPath {
    override val isRoot  = false
    override val isIndex = true
    override val isField = false
  }

  def render(path: CodecPath): String = {
    @tailrec
    def go(remaining: CodecPath, acc: List[String]): List[String] =
      remaining match {
        case CodecPath.Root => acc
        case CodecPath.AtField(name, parent) =>
          go(parent, name :: acc)
        case CodecPath.AtIndex(idx, parent) =>
          go(parent, s"[$idx]" :: acc)
      }

    go(path, acc = Nil).mkString(".")
  }
}

case class HtmlTableEncoderContext(
  location:    CodecPath,
  headers:     List[String],
  fieldStyles: String => List[Modifier]) { self =>

  def parentLocation: CodecPath = location.parent

  def enterField(name: String): HtmlTableEncoderContext =
    copy(location = CodecPath.AtField(name, self.location))

  def enterIndex(idx: Int): HtmlTableEncoderContext =
    copy(location = CodecPath.AtIndex(idx, self.fieldLocation))

  private[html] def fieldLocation: CodecPath.AtField = location match {
    case x: CodecPath.AtField => x
    case _                    => sys.error("Library error, please fill a ticket")
  }

  def getFieldStyles: List[Modifier] =
    if (location.isField) fieldStyles(fieldLocation.name)
    else Nil

  def pathStr: String = location.toString
}

object HtmlTableEncoderContext {
  def initial(
    headers:     List[String],
    fieldStyles: String => List[Modifier]
  ): HtmlTableEncoderContext =
    HtmlTableEncoderContext(
      location = CodecPath.Root,
      headers = headers,
      fieldStyles = fieldStyles
    )
}