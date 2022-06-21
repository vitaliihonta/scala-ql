package scalaql.utils

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

abstract class MacroUtils(val c: blackbox.Context)(prefix: String) {
  import c.universe.*

  // TODO: handle .each
  protected def extractSelectorField(t: Tree): Option[TermName] =
    t match {
      case q"(${vd: ValDef}) => ${idt: Ident}.${fieldName: TermName}" if vd.name == idt.name =>
        Some(fieldName)
      case q"(${vd: ValDef}) => ${idt: Ident}.${fieldName: TermName}" if vd.name == idt.name =>
        Some(fieldName)
      case _ =>
        None
    }

  protected def error(message: String): Nothing = c.abort(c.enclosingPosition, message)

  protected def debugEnabled: Boolean =
    sys.props
      .get(s"scala-ql-$prefix.debug.macro")
      .flatMap(str => scala.util.Try(str.toBoolean).toOption)
      .getOrElse(false)

  implicit class Debugged[A](self: A) {

    def debugged(msg: String): A = {
      if (debugEnabled) {
        println(s"$msg: $self")
      }
      self
    }
  }
}
