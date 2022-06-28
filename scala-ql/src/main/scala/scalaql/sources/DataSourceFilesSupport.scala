package scalaql.sources

import scalaql.SideEffect
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileSystems, Files, OpenOption, Path}
import scala.jdk.CollectionConverters.*

trait DataSourceReaderFilesSupport[Source <: AutoCloseable, Decoder[_], Config[_]] {
  this: DataSourceReader[Source, Decoder, Config] =>

  protected[scalaql] def openFile(path: Path, encoding: Charset): Source

}

trait DataSourceFilesReadDslMixin[
  A,
  Source <: AutoCloseable,
  Decoder[_],
  Config[_],
  DSReader <: DataSourceReader[Source, Decoder, Config] & DataSourceReaderFilesSupport[Source, Decoder, Config],
  Self <: DataSourceReadDsl[A, Source, Decoder, Config, DSReader, Self]] { self: Self =>

  def file(
    path:        Path,
    encoding:    Charset = StandardCharsets.UTF_8
  )(implicit ev: Decoder[A]
  ): Iterable[A] = self.load(_reader.openFile(path, encoding))

  def files(
    encoding:    Charset = StandardCharsets.UTF_8
  )(files:       Path*
  )(implicit ev: Decoder[A]
  ): Iterable[A] =
    files.flatMap(file(_, encoding))

  // NOTE: globPattern should not include `glob:`
  def directory(
    dir:         Path,
    globPattern: String,
    maxDepth:    Int = 20,
    encoding:    Charset = StandardCharsets.UTF_8
  )(implicit ev: Decoder[A]
  ): Iterable[A] = {

    val pathMatcher = FileSystems.getDefault.getPathMatcher(
      s"glob:$globPattern"
    )

    Files
      .find(
        dir,
        maxDepth,
        (p: Path, attrs: BasicFileAttributes) => !attrs.isDirectory && pathMatcher.matches(p)
      )
      .iterator()
      .asScala
      .flatMap(file(_, encoding))
      .toVector
  }
}

trait DataSourceWriterFilesSupport[Sink, Encoder[_], Config[_]] {
  this: DataSourceWriter[Sink, Encoder, Config] =>

  protected[scalaql] def openFile(path: Path, encoding: Charset, openOptions: OpenOption*): Sink

}

trait DataSourceFilesWriteDslMixin[
  A,
  Sink,
  Encoder[_],
  Config[_],
  DSWriter <: DataSourceWriter[Sink, Encoder, Config] & DataSourceWriterFilesSupport[Sink, Encoder, Config],
  Self <: DataSourceWriteDsl[A, Sink, Encoder, Config, DSWriter, Self]] { self: Self =>

  def file(
    path:        Path
  )(implicit ev: Encoder[A]
  ): SideEffect[Sink, ?, A] =
    file(path, encoding = StandardCharsets.UTF_8)

  def file(
    path:        Path,
    encoding:    Charset,
    openOptions: OpenOption*
  )(implicit ev: Encoder[A]
  ): SideEffect[Sink, ?, A] =
    save(_writer.openFile(path, encoding, openOptions: _*))
}
