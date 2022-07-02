package scalaql.json

import io.circe.Decoder
import scalaql.json.internal.JsonDataSourceReader
import scalaql.sources.*
import java.io.Reader

class JsonReadDsl[A](override val config: JsonReadConfig)
    extends DataSourceReadDsl[A, Reader, Decoder, λ[a => JsonReadConfig], JsonDataSourceReader, JsonReadDsl[A]]
    with DataSourceJavaIOReadDslMixin[A, Decoder, λ[a => JsonReadConfig], JsonDataSourceReader, JsonReadDsl[A]]
    with DataSourceFilesReadDslMixin[A, Reader, Decoder, λ[a => JsonReadConfig], JsonDataSourceReader, JsonReadDsl[A]]
    with DataSourceHttpReadDslMixin[A, Reader, Decoder, λ[a => JsonReadConfig], JsonDataSourceReader, JsonReadDsl[A]] {

  override protected val _reader = JsonDataSourceReader

  override def withConfig(config: JsonReadConfig): JsonReadDsl[A] =
    new JsonReadDsl[A](config)

  def option(multiline: Boolean): JsonReadDsl[A] =
    withConfig(config.copy(multiline = multiline))

  def option(lineTerminator: String): JsonReadDsl[A] =
    withConfig(config.copy(lineTerminator = lineTerminator))

  def options(
    multiline:      Boolean = config.multiline,
    lineTerminator: String = config.lineTerminator
  ): JsonReadDsl[A] =
    withConfig(
      config.copy(
        multiline = multiline,
        lineTerminator = lineTerminator
      )
    )
}
