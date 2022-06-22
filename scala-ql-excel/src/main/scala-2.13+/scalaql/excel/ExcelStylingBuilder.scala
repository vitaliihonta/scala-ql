package scalaql.excel

import org.apache.poi.ss.usermodel.CellStyle
import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import scalaql.utils.BaseTypeCheckedBuilder
import scala.util.Try

class ExcelStylingBuilder[A](
  headerStyles: Either[String => Option[Styling], Map[String, Styling]] = Right(Map.empty[String, Styling]),
  cellStyles:   Map[String, Styling] = Map.empty) { self =>

  def build: ExcelStyling[A] =
    new ExcelStyling.Configured[A](
      (name: String) => headerStyles.map(hs => Try(hs(name)).toOption).left.map(_.apply(name)).merge,
      (name: String) => Try(cellStyles(name)).toOption
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

  def withDefaultForFields(styling: Styling): ExcelStylingBuilder[A] =
    new ExcelStylingBuilder[A](
      headerStyles = self.headerStyles,
      cellStyles = cellStyles.withDefaultValue(styling)
    )

  def withDefaultForHeaders(styling: Styling): ExcelStylingBuilder[A] =
    new ExcelStylingBuilder[A](
      headerStyles = self.headerStyles.map(_.withDefaultValue(styling)),
      cellStyles = self.cellStyles
    )

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
