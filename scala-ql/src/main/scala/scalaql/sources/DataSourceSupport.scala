package scalaql.sources

import scalaql.SideEffect

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.StringReader
import java.io.Writer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

trait DataSourceSupport[Source <: AutoCloseable, Sink, Decoder[_], Encoder[_], Config]
    extends DataSourceReadSupport[Source, Decoder, Config]
    with DataSourceWriteSupport[Sink, Encoder, Config]

trait DataSourceJavaIOSupport[Decoder[_], Encoder[_], Config]
    extends DataSourceSupport[Reader, Writer, Decoder, Encoder, Config]

trait DataSourceReadSupport[Source <: AutoCloseable, Decoder[_], Config] {
  val read: DataSourceReader[Source, Decoder, Config]
}

trait DataSourceJavaIOReadSupport[Decoder[_], Config] extends DataSourceReadSupport[Reader, Decoder, Config]
trait DataSourceJavaInputStreamReadSupport[Decoder[_], Config]
    extends DataSourceReadSupport[InputStream, Decoder, Config]

trait DataSourceWriteSupport[Sink, Encoder[_], Config] {
  val write: DataSourceWriter[Sink, Encoder, Config]
}

trait DataSourceJavaIOWriteSupport[Encoder[_], Config] extends DataSourceWriteSupport[Writer, Encoder, Config]

trait DataSourceReader[Source <: AutoCloseable, Decoder[_], Config] {

  /** Implement reading logic here. NOTE - no need to close the reader, it's handled in public methods
    */
  protected def readImpl[A: Decoder](source: Source)(implicit config: Config): Iterable[A]

  def read[A: Decoder](
    source:          => Source
  )(implicit config: Config
  ): Iterable[A] = {
    val theSource = source
    try readImpl[A](theSource)
    finally theSource.close()
  }
}

trait DataSourceJavaIOReader[Decoder[_], Config] extends DataSourceReader[Reader, Decoder, Config] {

  // suitable for unit tests
  def string[A: Decoder](
    content:         String
  )(implicit config: Config
  ): Iterable[A] = read(new StringReader(content))
}

trait DataSourceJavaInputStreamReader[Decoder[_], Config] extends DataSourceReader[InputStream, Decoder, Config]

trait DataSourceWriter[Sink, Encoder[_], Config] {
  def write[A: Encoder](
    sink:            => Sink
  )(implicit config: Config
  ): SideEffect[?, ?, A]
}

trait DataSourceJavaIOWriter[Encoder[_], Config] extends DataSourceWriter[Writer, Encoder, Config] {

  def string[A: Encoder](
    builder:         mutable.StringBuilder
  )(implicit config: Config
  ): SideEffect[?, ?, A] = {
    val baos = new ByteArrayOutputStream()
    write(new OutputStreamWriter(baos))
      .onExit {
        builder.append(new String(baos.toByteArray))
        baos.close()
      }
  }
}

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

trait DataSourceJavaIOReaderFilesSupport[Decoder[_], Config]
    extends DataSourceReaderFilesSupport[Reader, Decoder, Config] {
  this: DataSourceReader[Reader, Decoder, Config] =>

  override def openFile(path: Path, encoding: Charset): Reader =
    Files.newBufferedReader(path, encoding)
}

trait DataSourceJavaInputStreamReaderFilesSupport[Decoder[_], Config]
    extends DataSourceReaderFilesSupport[InputStream, Decoder, Config] {
  this: DataSourceReader[InputStream, Decoder, Config] =>

  override def openFile(path: Path, encoding: Charset): InputStream =
    Files.newInputStream(path)
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

trait DataSourceJavaIOWriterFilesSupport[Encoder[_], Config]
    extends DataSourceWriterFilesSupport[Writer, Encoder, Config] {
  this: DataSourceWriter[Writer, Encoder, Config] =>

  override def openFile(path: Path, encoding: Charset, openOptions: OpenOption*): Writer =
    Files.newBufferedWriter(path, encoding, openOptions: _*)

}

trait DataSourceJavaOuputStreamWriterFilesSupport[Encoder[_], Config]
    extends DataSourceWriterFilesSupport[OutputStream, Encoder, Config] {
  this: DataSourceWriter[OutputStream, Encoder, Config] =>

  override def openFile(path: Path, encoding: Charset, openOptions: OpenOption*): OutputStream =
    Files.newOutputStream(path, openOptions: _*)

}
