package scalaql.sources.columnar

import scala.annotation.tailrec

sealed trait CodecPath {
  def isRoot: Boolean
  def isIndex: Boolean
  def isField: Boolean
  def parent: CodecPath

  protected[scalaql] def fieldLocation: CodecPath.AtField =
    sys.error("Library error, please fill a bug ticket")

  override def toString: String = {
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
    override def toString: String  = "root"
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
