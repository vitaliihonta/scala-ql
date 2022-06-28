package scalaql.json

import io.circe.Encoder
import scalaql.json.internal.JsonDataSourceWriter
import scalaql.sources.{DataSourceFilesWriteDslMixin, DataSourceJavaIOWriteDslMixin, DataSourceWriteDsl}

import java.io.Writer

class JsonWriteDsl[A](override val config: JsonWriteConfig)
    extends DataSourceWriteDsl[A, Writer, Encoder, λ[a => JsonWriteConfig], JsonDataSourceWriter, JsonWriteDsl[A]]
    with DataSourceJavaIOWriteDslMixin[A, Encoder, λ[a => JsonWriteConfig], JsonDataSourceWriter, JsonWriteDsl[A]]
    with DataSourceFilesWriteDslMixin[A, Writer, Encoder, λ[a => JsonWriteConfig], JsonDataSourceWriter, JsonWriteDsl[
      A
    ]] {

  override protected val _writer = JsonDataSourceWriter

  override def withConfig(config: JsonWriteConfig): JsonWriteDsl[A] =
    new JsonWriteDsl[A](config)

  def option(multiline: Boolean): JsonWriteDsl[A] =
    withConfig(config.copy(multiline = multiline))

  def option(lineTerminator: String): JsonWriteDsl[A] =
    withConfig(config.copy(lineTerminator = lineTerminator))

  def options(
    multiline:      Boolean = config.multiline,
    lineTerminator: String = config.lineTerminator
  ): JsonWriteDsl[A] =
    withConfig(
      config.copy(
        multiline = multiline,
        lineTerminator = lineTerminator
      )
    )
}
