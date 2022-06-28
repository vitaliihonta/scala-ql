package scalaql.sources

import scalaql.SideEffect

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import scala.collection.mutable

trait DataSourceJavaStreamsSupport[
  Decoder[_],
  Encoder[_],
  ReadConfig[_],
  WriteConfig[_],
  DSReader <: DataSourceReader[InputStream, Decoder, ReadConfig],
  DSWriter <: DataSourceWriter[OutputStream, Encoder, WriteConfig],
  ReadDSL[A] <: DataSourceReadDsl[A, InputStream, Decoder, ReadConfig, DSReader, ReadDSL[A]],
  WriteDSL[A] <: DataSourceWriteDsl[A, OutputStream, Encoder, WriteConfig, DSWriter, WriteDSL[A]]]
    extends DataSourceSupport[
      InputStream,
      OutputStream,
      Decoder,
      Encoder,
      ReadConfig,
      WriteConfig,
      DSReader,
      DSWriter,
      ReadDSL,
      WriteDSL
    ]

trait DataSourceJavaInputStreamReadSupport[
  Decoder[_],
  Config[_],
  DSReader <: DataSourceReader[InputStream, Decoder, Config],
  ReadDSL[A] <: DataSourceReadDsl[A, InputStream, Decoder, Config, DSReader, ReadDSL[A]]]
    extends DataSourceReadSupport[InputStream, Decoder, Config, DSReader, ReadDSL]

trait DataSourceJavaOutputStreamWriteSupport[
  Encoder[_],
  Config[_],
  DSWriter <: DataSourceWriter[OutputStream, Encoder, Config],
  WriteDSL[A] <: DataSourceWriteDsl[A, OutputStream, Encoder, Config, DSWriter, WriteDSL[A]]]
    extends DataSourceWriteSupport[OutputStream, Encoder, Config, DSWriter, WriteDSL]

trait DataSourceJavaInputStreamReader[Decoder[_], Config[_]] extends DataSourceReader[InputStream, Decoder, Config]

trait DataSourceJavaInputStreamReadDslMixin[
  A,
  Decoder[_],
  Config[_],
  DSReader <: DataSourceReader[InputStream, Decoder, Config],
  Self <: DataSourceReadDsl[A, InputStream, Decoder, Config, DSReader, Self]] {

  self: Self =>

  def string(content: String)(implicit ev: Decoder[A]): Iterable[A] =
    load(new ByteArrayInputStream(content.getBytes))
}

trait DataSourceJavaOutputStreamWriteDslMixin[
  A,
  Encoder[_],
  Config[_],
  DSWriter <: DataSourceWriter[OutputStream, Encoder, Config],
  Self <: DataSourceWriteDsl[A, OutputStream, Encoder, Config, DSWriter, Self]] {

  self: Self =>

  def string(builder: mutable.StringBuilder)(implicit ev: Encoder[A]): SideEffect[OutputStream, ?, A] =
    save(new ByteArrayOutputStream())
      .afterAll { (os, _) =>
        builder.append(new String(os.asInstanceOf[ByteArrayOutputStream].toByteArray))
      }
}

trait DataSourceJavaOutputStreamWriter[Encoder[_], Config[_]] extends DataSourceWriter[OutputStream, Encoder, Config]

trait DataSourceJavaInputStreamReaderFilesSupport[Decoder[_], Config[_]]
    extends DataSourceReaderFilesSupport[InputStream, Decoder, Config] {
  this: DataSourceReader[InputStream, Decoder, Config] =>

  override def openFile(path: Path, encoding: Charset): InputStream =
    Files.newInputStream(path)
}

trait DataSourceJavaOutputStreamWriteFilesSupport[Encoder[_], Config[_]]
    extends DataSourceWriterFilesSupport[OutputStream, Encoder, Config] {
  this: DataSourceWriter[OutputStream, Encoder, Config] =>

  override def openFile(path: Path, encoding: Charset, openOptions: OpenOption*): OutputStream =
    Files.newOutputStream(path, openOptions: _*)

}
