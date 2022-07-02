package scalaql.sources

import java.io.InputStream
import java.net.URL
import java.nio.charset.{Charset, StandardCharsets}

trait DataSourceReaderHttpSupport[Source <: AutoCloseable, Decoder[_], Config[_]] {

  protected[scalaql] def fromInputStream(is: InputStream, encoding: Charset): Source
}

trait DataSourceHttpReadDslMixin[
  A,
  Source <: AutoCloseable,
  Decoder[_],
  Config[_],
  DSReader <: DataSourceReader[Source, Decoder, Config] & DataSourceReaderHttpSupport[Source, Decoder, Config],
  Self <: DataSourceReadDsl[A, Source, Decoder, Config, DSReader, Self]] {
  self: Self =>

  def url(url: URL, encoding: Charset = StandardCharsets.UTF_8)(implicit ev: Decoder[A]): Iterable[A] =
    self.load(
      _reader.fromInputStream(url.openStream(), encoding)
    )
}
