package scalaql.sources

import scalaql.SideEffect
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import scala.jdk.CollectionConverters.*

trait DataSourceReaderFilesSupport[Source <: AutoCloseable, Decoder[_], Config] {
  this: DataSourceReader[Source, Decoder, Config] =>

  protected def openFile(path: Path, encoding: Charset): Source

  def file[A: Decoder](
    path:            Path,
    encoding:        Charset = StandardCharsets.UTF_8
  )(implicit config: Config
  ): Iterable[A] = read(openFile(path, encoding))

  def files[A: Decoder](
    encoding:        Charset = StandardCharsets.UTF_8
  )(files:           Path*
  )(implicit config: Config
  ): Iterable[A] =
    files.flatMap(file[A](_, encoding))

  def directory[A: Decoder](
    dir:             Path,
    globPattern:     String = "*",
    encoding:        Charset = StandardCharsets.UTF_8
  )(implicit config: Config
  ): Iterable[A] =
    fromDirectoryStream[A](Files.newDirectoryStream(dir, globPattern), encoding)

  private def fromDirectoryStream[A: Decoder](
    dirStream:       DirectoryStream[Path],
    encoding:        Charset
  )(implicit config: Config
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

trait DataSourceWriterFilesSupport[Sink, Encoder[_], Config] {
  this: DataSourceWriter[Sink, Encoder, Config] =>

  protected def openFile(path: Path, encoding: Charset, openOptions: OpenOption*): Sink

  def file[A: Encoder](
    path:            Path
  )(implicit config: Config
  ): SideEffect[?, ?, A] =
    file(path, encoding = StandardCharsets.UTF_8)

  def file[A: Encoder](
    path:            Path,
    encoding:        Charset,
    openOptions:     OpenOption*
  )(implicit config: Config
  ): SideEffect[?, ?, A] =
    write(openFile(path, encoding, openOptions: _*))
}
