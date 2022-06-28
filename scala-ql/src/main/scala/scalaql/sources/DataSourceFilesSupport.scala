package scalaql.sources

import scalaql.SideEffect

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{DirectoryStream, FileSystems, Files, OpenOption, Path}
import scala.jdk.CollectionConverters.*

trait DataSourceReaderFilesSupport[Source <: AutoCloseable, Decoder[_], Config[_]] {
  this: DataSourceReader[Source, Decoder, Config] =>

  protected def openFile(path: Path, encoding: Charset): Source

  def file[A: Decoder](
    path:            Path,
    encoding:        Charset = StandardCharsets.UTF_8
  )(implicit config: Config[A]
  ): Iterable[A] = read(openFile(path, encoding))

  def files[A: Decoder](
    encoding:        Charset = StandardCharsets.UTF_8
  )(files:           Path*
  )(implicit config: Config[A]
  ): Iterable[A] =
    files.flatMap(file[A](_, encoding))

  // NOTE: globPattern should not include `glob:`
  def directory[A: Decoder](
    dir:             Path,
    globPattern:     String,
    maxDepth:        Int = 20,
    encoding:        Charset = StandardCharsets.UTF_8
  )(implicit config: Config[A]
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
      .flatMap(file[A](_, encoding))
      .toVector
  }

  private def fromDirectoryStream[A: Decoder](
    dirStream:       DirectoryStream[Path],
    encoding:        Charset
  )(implicit config: Config[A]
  ): Iterable[A] =
    try
      dirStream
        .iterator()
        .asScala
        .flatMap(file[A](_, encoding))
        .toVector
    finally
      dirStream.close()
}

trait DataSourceWriterFilesSupport[Sink, Encoder[_], Config[_]] {
  this: DataSourceWriter[Sink, Encoder, Config] =>

  protected def openFile(path: Path, encoding: Charset, openOptions: OpenOption*): Sink

  def file[A: Encoder](
    path:            Path
  )(implicit config: Config[A]
  ): SideEffect[?, ?, A] =
    file(path, encoding = StandardCharsets.UTF_8)

  def file[A: Encoder](
    path:            Path,
    encoding:        Charset,
    openOptions:     OpenOption*
  )(implicit config: Config[A]
  ): SideEffect[?, ?, A] =
    write(openFile(path, encoding, openOptions: _*))
}
