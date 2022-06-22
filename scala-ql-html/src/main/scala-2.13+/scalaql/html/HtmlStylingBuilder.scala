package scalaql.html

import scalatags.Text.all.*
import scalaql.utils.BaseTypeCheckedBuilder
import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import scala.util.Try

class HtmlStylingBuilder[A](
  headerStyles: Either[String => List[Modifier], Map[String, List[Modifier]]] = Right(
    Map.empty[String, List[Modifier]]
  ),
  fieldStyles: Map[String, List[Modifier]] = Map.empty) { self =>

  def build: HtmlStyling[A] =
    new HtmlStyling.Configured[A](
      (name: String) =>
        Try(headerStyles.map(_.apply(name))).toOption.map(_.left.map(_.apply(name)).merge).toList.flatten,
      (name: String) => Try(fieldStyles(name)).toOption.toList.flatten
    )

  def forAllHeaders(styling: List[Modifier]): HtmlStylingBuilder[A] =
    new HtmlStylingBuilder[A](
      headerStyles = Left((_: String) => styling),
      fieldStyles = self.fieldStyles
    )

  def forAllFields(stylings: (String, List[Modifier])*): HtmlStylingBuilder[A] =
    new HtmlStylingBuilder[A](
      headerStyles = self.headerStyles,
      fieldStyles = stylings.toMap
    )

  def forHeader[B](f: A => B, styling: List[Modifier]): HtmlStylingBuilder[A] =
    macro HtmlStylingBuilderMacro.forHeaderImpl[A, B]

  def forField[B](f: A => B, styling: List[Modifier]): HtmlStylingBuilder[A] =
    macro HtmlStylingBuilderMacro.forFieldImpl[A, B]

  def withDefaultForFields(styling: List[Modifier]): HtmlStylingBuilder[A] =
    new HtmlStylingBuilder[A](
      headerStyles = self.headerStyles,
      fieldStyles = fieldStyles.withDefaultValue(styling)
    )

  def withDefaultForHeaders(styling: List[Modifier]): HtmlStylingBuilder[A] =
    new HtmlStylingBuilder[A](
      headerStyles = self.headerStyles.map(_.withDefaultValue(styling)),
      fieldStyles = self.fieldStyles
    )

  def addHeaderStyle(headerName: String, styling: List[Modifier]): HtmlStylingBuilder[A] =
    new HtmlStylingBuilder[A](
      headerStyles = Right(
        self.headerStyles.getOrElse(Map.empty[String, List[Modifier]]).updated(headerName, styling)
      ),
      fieldStyles = self.fieldStyles
    )

  def addFieldStyle(fieldName: String, styling: List[Modifier]): HtmlStylingBuilder[A] =
    new HtmlStylingBuilder[A](
      headerStyles = self.headerStyles,
      fieldStyles = self.fieldStyles.updated(fieldName, styling)
    )
}

class HtmlStylingBuilderMacro(override val c: blackbox.Context)
    extends BaseTypeCheckedBuilder[HtmlStylingBuilder](c)("html") {

  import c.universe.*

  def forHeaderImpl[A: WeakTypeTag, B: WeakTypeTag](f: Expr[A => B], styling: Expr[List[Modifier]]): Tree =
    builderStepImpl[A, B](f)((prefix, fieldName) => q"""$prefix.addHeaderStyle($fieldName, $styling)""")

  def forFieldImpl[A: WeakTypeTag, B: WeakTypeTag](f: Expr[A => B], styling: Expr[List[Modifier]]): Tree =
    builderStepImpl[A, B](f)((prefix, fieldName) => q"""$prefix.addFieldStyle($fieldName, $styling)""")

}
