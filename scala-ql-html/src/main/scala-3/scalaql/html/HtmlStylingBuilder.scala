package scalaql.html

import scalatags.Text.all.*
import scala.quoted.*
import scalaql.utils.Scala3MacroUtils
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

object HtmlStylingBuilder {
  extension [A](self: HtmlStylingBuilder[A]) {
    inline def forHeader[B](f: A => B, styling: List[Modifier]): HtmlStylingBuilder[A] =
      ${ forHeaderImpl[A, B]('self, 'f, 'styling) }

    inline def forField[B](f: A => B, styling: List[Modifier]): HtmlStylingBuilder[A] =
      ${ forFieldImpl[A, B]('self, 'f, 'styling) }
  }

  def forHeaderImpl[A: Type, B](
    self:    Expr[HtmlStylingBuilder[A]],
    f:       Expr[A => B],
    styling: Expr[List[Modifier]]
  )(using Quotes
  ): Expr[HtmlStylingBuilder[A]] = {
    val fieldName = Expr(Scala3MacroUtils.accessorName[A, B](f))
    '{ $self.addHeaderStyle($fieldName, $styling) }
  }

  def forFieldImpl[A: Type, B](
    self:    Expr[HtmlStylingBuilder[A]],
    f:       Expr[A => B],
    styling: Expr[List[Modifier]]
  )(using Quotes
  ): Expr[HtmlStylingBuilder[A]] = {
    val fieldName = Expr(Scala3MacroUtils.accessorName[A, B](f))
    '{ $self.addFieldStyle($fieldName, $styling) }
  }
}
