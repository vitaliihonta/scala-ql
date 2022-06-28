package scalaql.html

import scalaql.html.internal.HtmlDataSourceWriter
import scalaql.sources.*

trait ScalaqlHtmlSupport
    extends DataSourceJavaIOWriteSupport[
      HtmlTableEncoder,
      HtmlTableEncoderConfig,
      HtmlDataSourceWriter,
      HtmlWriteDsl
    ] {

  override def write[A]: HtmlWriteDsl[A] = new HtmlWriteDsl[A](HtmlTableEncoderConfig.default)
}
