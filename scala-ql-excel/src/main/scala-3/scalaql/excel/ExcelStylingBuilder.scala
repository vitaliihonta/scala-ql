package scalaql.excel

import scala.quoted.*

class ExcelStylingBuilder[A](
  headerStyles: Either[String => Option[Styling], Map[String, Styling]] = Right(Map.empty[String, Styling]),
  cellStyles:   Map[String, Styling] = Map.empty) {
  self =>

  def build: ExcelStyling[A] = new ExcelStyling.Configured[A](
    (name: String) => headerStyles.map(_.get(name)).left.map(_.apply(name)).merge,
    cellStyles.get(_: String)
  )

  def forAllHeaders(styling: Styling): ExcelStylingBuilder[A] = new ExcelStylingBuilder[A](
    headerStyles = Left((_: String) => Some(styling)),
    cellStyles = self.cellStyles
  )

  def forAllFields(stylings: (String, Styling)*): ExcelStylingBuilder[A] =
    new ExcelStylingBuilder[A](
      headerStyles = self.headerStyles,
      cellStyles = stylings.toMap
    )

  // TODO: implement
  inline def forHeader[B](f: A => B, styling: Styling): ExcelStylingBuilder[A] =
    ???

  // TODO: implement
  inline def forField[B](f: A => B, styling: Styling): ExcelStylingBuilder[A] =
    ???

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

private[excel] object ExcelStylingBuilderHelperScala3 {
  def forHeaderImpl[A, B](
    self:    Expr[ExcelStylingBuilder[A]],
    f:       Expr[A => B],
    styling: Expr[Styling]
  ): Expr[ExcelStylingBuilder[A]] = self

  def forFieldImpl[A, B](
    self:    Expr[ExcelStylingBuilder[A]],
    f:       Expr[A => B],
    styling: Expr[Styling]
  ): Expr[ExcelStylingBuilder[A]] = self

  def accessorName[A, B](f: Expr[A => B])(using Quotes): Expr[String] = {
    import quotes.reflect.*
    Expr.betaReduce(f).asTerm.underlying match {
      case Block(List(DefDef(_, _, _, Some(Select(Ident(_), name)))), _) =>
        Expr(name)
      case _ =>
        throw new IllegalArgumentException(s"Expected a field selector to be passed (as instance.field1), got $f")
    }
  }
}