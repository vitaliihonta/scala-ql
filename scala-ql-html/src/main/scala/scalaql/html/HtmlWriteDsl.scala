package scalaql.html

import scalaql.html.internal.HtmlDataSourceWriter
import scalaql.sources.{DataSourceFilesWriteDslMixin, DataSourceJavaIOWriteDslMixin, DataSourceWriteDsl, Naming}
import scalatags.Text.TypedTag
import java.io.Writer

class HtmlWriteDsl[A](override val _config: HtmlTableEncoderConfig[A])
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

  override def config(config: HtmlTableEncoderConfig[A]): HtmlWriteDsl[A] =
    new HtmlWriteDsl[A](config)

  def option(naming: Naming): HtmlWriteDsl[A] =
    config(_config.copy(naming = naming))

  def option(styling: HtmlStyling[A]): HtmlWriteDsl[A] =
    config(_config.copy(styling = styling))

  def options(
    htmlTag:  TypedTag[String] = _config.htmlTag,
    headTag:  TypedTag[String] = _config.headTag,
    bodyTag:  TypedTag[String] = _config.bodyTag,
    tableTag: TypedTag[String] = _config.tableTag,
    trTag:    TypedTag[String] = _config.trTag,
    thTag:    TypedTag[String] = _config.thTag,
    tdTag:    TypedTag[String] = _config.tdTag
  ): HtmlWriteDsl[A] =
    config(
      _config.copy(
        htmlTag = htmlTag,
        headTag = headTag,
        bodyTag = bodyTag,
        tableTag = tableTag,
        trTag = trTag,
        thTag = thTag,
        tdTag = tdTag
      )
    )
}
