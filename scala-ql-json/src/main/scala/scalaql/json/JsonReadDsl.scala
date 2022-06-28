package scalaql.json

import io.circe.Decoder
import scalaql.json.internal.JsonDataSourceReader
import scalaql.sources.{DataSourceFilesReadDslMixin, DataSourceJavaIOReadDslMixin, DataSourceReadDsl}
import java.io.Reader

class JsonReadDsl[A](override protected val _config: JsonReadConfig)
    extends DataSourceReadDsl[A, Reader, Decoder, λ[a => JsonReadConfig], JsonDataSourceReader, JsonReadDsl[A]]
    with DataSourceJavaIOReadDslMixin[A, Decoder, λ[a => JsonReadConfig], JsonDataSourceReader, JsonReadDsl[A]]
    with DataSourceFilesReadDslMixin[A, Reader, Decoder, λ[a => JsonReadConfig], JsonDataSourceReader, JsonReadDsl[A]] {

  override protected val _reader = JsonDataSourceReader

  override def config(config: JsonReadConfig): JsonReadDsl[A] =
    new JsonReadDsl[A](config)

  def option(multiline: Boolean): JsonReadDsl[A] =
    config(_config.copy(multiline = multiline))

  def option(lineTerminator: String): JsonReadDsl[A] =
    config(_config.copy(lineTerminator = lineTerminator))
}
