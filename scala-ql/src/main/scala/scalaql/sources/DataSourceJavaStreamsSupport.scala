package scalaql.sources

import scalaql.SideEffect
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import scala.collection.mutable

trait DataSourceJavaStreamsSupport[Decoder[_], Encoder[_], ReadConfig, WriteConfig]
    extends DataSourceSupport[InputStream, OutputStream, Decoder, Encoder, ReadConfig, WriteConfig]

trait DataSourceJavaInputStreamReadSupport[Decoder[_], Config]
    extends DataSourceReadSupport[InputStream, Decoder, Config]

trait DataSourceJavaOutputStreamWriteSupport[Encoder[_], Config]
    extends DataSourceWriteSupport[OutputStream, Encoder, Config]

trait DataSourceJavaInputStreamReader[Decoder[_], Config] extends DataSourceReader[InputStream, Decoder, Config]

trait DataSourceJavaOutputStreamWriter[Encoder[_], Config] extends DataSourceWriter[OutputStream, Encoder, Config] {

  def string[A: Encoder](
    builder:         mutable.StringBuilder
  )(implicit config: Config
  ): SideEffect[?, ?, A] = {
    val baos = new ByteArrayOutputStream()
    write(baos)
      .onExit {
        builder.append(new String(baos.toByteArray))
        baos.close()
      }
  }
}

trait DataSourceJavaInputStreamReaderFilesSupport[Decoder[_], Config]
    extends DataSourceReaderFilesSupport[InputStream, Decoder, Config] {
  this: DataSourceReader[InputStream, Decoder, Config] =>

  override def openFile(path: Path, encoding: Charset): InputStream =
    Files.newInputStream(path)
}

trait DataSourceJavaOutputStreamWriteFilesSupport[Encoder[_], Config]
    extends DataSourceWriterFilesSupport[OutputStream, Encoder, Config] {
  this: DataSourceWriter[OutputStream, Encoder, Config] =>

  override def openFile(path: Path, encoding: Charset, openOptions: OpenOption*): OutputStream =
    Files.newOutputStream(path, openOptions: _*)

}
