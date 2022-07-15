package scalaql.excel

import org.apache.poi.ss.usermodel.CellStyle
import scalaql.internal.BaseTypeCheckedBuilder
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

class ExcelStylingBuilderMacro(override val c: blackbox.Context)
    extends BaseTypeCheckedBuilder[ExcelStylingBuilder](c)("excel") {

  import c.universe.*

  def forHeaderImpl[A: WeakTypeTag, B: WeakTypeTag](f: Expr[A => B], styling: Expr[Styling]): Tree =
    builderStepImpl[A, B](f)((prefix, fieldName) => q"""$prefix.addHeaderStyle($fieldName, $styling)""")

  def forFieldImpl[A: WeakTypeTag, B: WeakTypeTag](f: Expr[A => B], styling: Expr[Styling]): Tree =
    builderStepImpl[A, B](f)((prefix, fieldName) => q"""$prefix.addFieldStyle($fieldName, $styling)""")

}
