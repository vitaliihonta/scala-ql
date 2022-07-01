package scalaql.html

import scalaql.Naming
import scalaql.html.internal.HtmlDataSourceWriter
import scalaql.sources.{DataSourceFilesWriteDslMixin, DataSourceJavaIOWriteDslMixin, DataSourceWriteDsl}
import scalatags.Text.TypedTag
import java.io.Writer

class HtmlWriteDsl[A](override val config: HtmlTableEncoderConfig[A])
    extends DataSourceWriteDsl[A, Writer, HtmlTableEncoder, HtmlTableEncoderConfig, HtmlDataSourceWriter, HtmlWriteDsl[
      A
    ]]
    with DataSourceJavaIOWriteDslMixin[A, HtmlTableEncoder, HtmlTableEncoderConfig, HtmlDataSourceWriter, HtmlWriteDsl[
      A
    ]]
    with DataSourceFilesWriteDslMixin[
      A,
      Writer,
      HtmlTableEncoder,
      HtmlTableEncoderConfig,
      HtmlDataSourceWriter,
      HtmlWriteDsl[
        A
      ]
    ] {

  override protected val _writer = HtmlDataSourceWriter

  override def withConfig(config: HtmlTableEncoderConfig[A]): HtmlWriteDsl[A] =
    new HtmlWriteDsl[A](config)

  def option(naming: Naming): HtmlWriteDsl[A] =
    withConfig(config.copy(naming = naming))

  def option(styling: HtmlStyling[A]): HtmlWriteDsl[A] =
    withConfig(config.copy(styling = styling))

  def options(
    htmlTag:  TypedTag[String] = config.htmlTag,
    headTag:  TypedTag[String] = config.headTag,
    bodyTag:  TypedTag[String] = config.bodyTag,
    tableTag: TypedTag[String] = config.tableTag,
    trTag:    TypedTag[String] = config.trTag,
    thTag:    TypedTag[String] = config.thTag,
    tdTag:    TypedTag[String] = config.tdTag,
    naming:   Naming = config.naming,
    styling:  HtmlStyling[A] = config.styling
  ): HtmlWriteDsl[A] =
    withConfig(
      config.copy(
        htmlTag = htmlTag,
        headTag = headTag,
        bodyTag = bodyTag,
        tableTag = tableTag,
        trTag = trTag,
        thTag = thTag,
        tdTag = tdTag,
        naming = naming,
        styling = styling
      )
    )
}
