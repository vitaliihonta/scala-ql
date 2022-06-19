package scalaql.sources

import scalaql.SideEffect

import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.StringReader
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import scala.collection.mutable

trait DataSourceJavaIOSupport[Decoder[_], Encoder[_], ReadConfig, WriteConfig]
    extends DataSourceSupport[Reader, Writer, Decoder, Encoder, ReadConfig, WriteConfig]

trait DataSourceJavaIOReadSupport[Decoder[_], Config] extends DataSourceReadSupport[Reader, Decoder, Config]

trait DataSourceJavaIOWriteSupport[Encoder[_], Config] extends DataSourceWriteSupport[Writer, Encoder, Config]

trait DataSourceJavaIOReader[Decoder[_], Config] extends DataSourceReader[Reader, Decoder, Config] {

  // suitable for unit tests
  def string[A: Decoder](
    content:         String
  )(implicit config: Config
  ): Iterable[A] = read(new StringReader(content))
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

trait DataSourceJavaIOReaderFilesSupport[Decoder[_], Config]
    extends DataSourceReaderFilesSupport[Reader, Decoder, Config] {
  this: DataSourceReader[Reader, Decoder, Config] =>

  override def openFile(path: Path, encoding: Charset): Reader =
    Files.newBufferedReader(path, encoding)
}

trait DataSourceJavaIOWriterFilesSupport[Encoder[_], Config]
    extends DataSourceWriterFilesSupport[Writer, Encoder, Config] {
  this: DataSourceWriter[Writer, Encoder, Config] =>

  override def openFile(path: Path, encoding: Charset, openOptions: OpenOption*): Writer =
    Files.newBufferedWriter(path, encoding, openOptions: _*)

}
