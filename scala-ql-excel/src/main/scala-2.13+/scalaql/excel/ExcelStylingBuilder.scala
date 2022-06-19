package scalaql.excel

import org.apache.poi.ss.usermodel.CellStyle

import scala.language.experimental.macros
import scala.reflect.macros.TypecheckException
import scala.reflect.macros.blackbox

class ExcelStylingBuilder[A](styles: Map[String, CellStyle => Unit] = Map.empty) { self =>

  def build: ExcelStyling[A] =
    new ExcelStyling.Configured[A](styles)

  def forField[B](f: A => B, style: CellStyle => Unit): ExcelStylingBuilder[A] =
    macro ExcelStylingBuilderMacro.forFieldImpl[A, B]

  def addStyle(fieldName: String, style: CellStyle => Unit): ExcelStylingBuilder[A] =
    new ExcelStylingBuilder[A](self.styles.updated(fieldName, style))
}

class ExcelStylingBuilderMacro(val c: blackbox.Context) {
  import c.universe.*

  def forFieldImpl[A: WeakTypeTag, B: WeakTypeTag](f: Expr[A => B], style: Expr[CellStyle => Unit]): Tree = {
    libraryUsageValidityCheck[A]()

    val fieldName = extractSelectorField(f.tree)
      .map(_.toString)
      .getOrElse(
        error(s"Expected a field selector to be passed (as instance.field1), got $f")
      )

    q"""${c.prefix.tree}.addStyle($fieldName, $style)"""
  }

  private def libraryUsageValidityCheck[A: WeakTypeTag](): Unit = {
    if (!(c.prefix.tree.tpe =:= weakTypeOf[ExcelStylingBuilder[A]])) {
      error("Invalid library usage! Refer to documentation")
    }
    val A           = weakTypeOf[A].dealias
    val tpe         = A.typeSymbol
    val isCaseClass = tpe.isClass && tpe.asClass.isCaseClass
    if (!isCaseClass) {
      error(s"Expected $A to be a case class")
    }
  }

  private def extractSelectorField(t: Tree): Option[TermName] =
    t match {
      case q"(${vd: ValDef}) => ${idt: Ident}.${fieldName: TermName}" if vd.name == idt.name =>
        Some(fieldName)
      case _ =>
        None
    }

  private def error(message: String): Nothing = c.abort(c.enclosingPosition, message)

  private def debugEnabled: Boolean =
    sys.props
      .get("scala-ql-excel.debug.macro")
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
