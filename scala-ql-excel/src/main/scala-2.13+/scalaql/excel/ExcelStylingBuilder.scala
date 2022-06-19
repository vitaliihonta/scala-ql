package scalaql.excel

import org.apache.poi.ss.usermodel.CellStyle
import scala.language.experimental.macros
import scala.reflect.macros.TypecheckException
import scala.reflect.macros.blackbox

class ExcelStylingBuilder[A](
  headerStyles: Either[String => Option[Styling], Map[String, Styling]] = Right(Map.empty[String, Styling]),
  cellStyles:   Map[String, Styling] = Map.empty) { self =>

  def build: ExcelStyling[A] =
    new ExcelStyling.Configured[A](
      (name: String) => headerStyles.map(_.get(name)).left.map(_.apply(name)).merge,
      cellStyles.get(_: String)
    )

  def forAllHeaders(styling: Styling): ExcelStylingBuilder[A] =
    new ExcelStylingBuilder[A](
      headerStyles = Left((_: String) => Some(styling)),
      cellStyles = self.cellStyles
    )

  def forAllFields(stylings: (String, Styling)*): ExcelStylingBuilder[A] =
    new ExcelStylingBuilder[A](
      headerStyles = self.headerStyles,
      cellStyles = stylings.toMap
    )

  def forHeader[B](f: A => B, styling: Styling): ExcelStylingBuilder[A] =
    macro ExcelStylingBuilderMacro.forHeaderImpl[A, B]

  def forField[B](f: A => B, styling: Styling): ExcelStylingBuilder[A] =
    macro ExcelStylingBuilderMacro.forFieldImpl[A, B]

  def addHeaderStyle(headerName: String, styling: Styling): ExcelStylingBuilder[A] =
    new ExcelStylingBuilder[A](
      headerStyles = Right(
        self.headerStyles.getOrElse(Map.empty[String, Styling]).updated(headerName, styling)
      ),
      cellStyles = self.cellStyles
    )

  def addFieldStyle(fieldName: String, styling: Styling): ExcelStylingBuilder[A] =
    new ExcelStylingBuilder[A](
      headerStyles = self.headerStyles,
      cellStyles = self.cellStyles.updated(fieldName, styling)
    )
}

class ExcelStylingBuilderMacro(val c: blackbox.Context) {
  import c.universe.*

  def forHeaderImpl[A: WeakTypeTag, B: WeakTypeTag](f: Expr[A => B], styling: Expr[Styling]): Tree =
    builderStepImpl[A, B](f)((prefix, fieldName) => q"""$prefix.addHeaderStyle($fieldName, $styling)""")

  def forFieldImpl[A: WeakTypeTag, B: WeakTypeTag](f: Expr[A => B], styling: Expr[Styling]): Tree =
    builderStepImpl[A, B](f)((prefix, fieldName) => q"""$prefix.addFieldStyle($fieldName, $styling)""")

  private def builderStepImpl[A: WeakTypeTag, B: WeakTypeTag](
    f:   Expr[A => B]
  )(use: (Tree, String) => Tree
  ): Tree = {
    libraryUsageValidityCheck[A]()

    val fieldName = extractSelectorField(f.tree)
      .map(_.toString)
      .getOrElse(
        error(s"Expected a field selector to be passed (as instance.field1), got $f")
      )

    use(c.prefix.tree, fieldName)
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
