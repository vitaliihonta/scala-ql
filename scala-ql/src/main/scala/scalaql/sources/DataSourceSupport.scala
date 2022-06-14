package scalaql.sources

import scalaql.SideEffect

import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
import java.io.Writer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import scala.collection.mutable

trait DataSourceSupport[Decoder[_], Encoder[_], Config] {
  val read: DataSourceReadSupport[Decoder, Config]
  val write: DataSourceWriteSupport[Encoder, Config]
}

trait DataSourceReadSupport[Decoder[_], Config] {

  /** Implement reading logic here. NOTE - no need to close the reader, it's handled in public methods
    */
  protected def readImpl[A: Decoder](reader: Reader)(implicit config: Config): Iterable[A]

  def read[A: Decoder](
    reader:          => Reader
  )(implicit config: Config
  ): Iterable[A] = {
    val theReader = reader
    try readImpl[A](theReader)
    finally theReader.close()
  }

  def file[A: Decoder](
    path:            Path
  )(implicit config: Config
  ): Iterable[A] = file(path, encoding = StandardCharsets.UTF_8)

  def file[A: Decoder](
    path:            Path,
    encoding:        Charset
  )(implicit config: Config
  ): Iterable[A] = read(Files.newBufferedReader(path, encoding))

  // suitable for unit tests
  def string[A: Decoder](
    content:         String
  )(implicit config: Config
  ): Iterable[A] = read(new StringReader(content))
}

trait DataSourceWriteSupport[Encoder[_], Config] {
  def write[A: Encoder](
    writer:          => Writer
  )(implicit config: Config
  ): SideEffect[?, ?, A]

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
    write(Files.newBufferedWriter(path, encoding, openOptions: _*))

  // suitable for unit tests
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
