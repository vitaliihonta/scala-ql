package scalaql.sources

import scalaql.SideEffect

import java.io.{ByteArrayOutputStream, OutputStreamWriter, Reader, StringReader, StringWriter, Writer}
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import scala.collection.mutable

trait DataSourceJavaIOSupport[
  Decoder[_],
  Encoder[_],
  ReadConfig[_],
  WriteConfig[_],
  DSReader <: DataSourceReader[Reader, Decoder, ReadConfig],
  DSWriter <: DataSourceWriter[Writer, Encoder, WriteConfig],
  ReadDSL[A] <: DataSourceReadDsl[A, Reader, Decoder, ReadConfig, DSReader, ReadDSL[A]],
  WriteDSL[A] <: DataSourceWriteDsl[A, Writer, Encoder, WriteConfig, DSWriter, WriteDSL[A]]]
    extends DataSourceSupport[
      Reader,
      Writer,
      Decoder,
      Encoder,
      ReadConfig,
      WriteConfig,
      DSReader,
      DSWriter,
      ReadDSL,
      WriteDSL
    ]

trait DataSourceJavaIOReadSupport[
  Decoder[_],
  Config[_],
  DSReader <: DataSourceReader[Reader, Decoder, Config],
  ReadDSL[A] <: DataSourceReadDsl[A, Reader, Decoder, Config, DSReader, ReadDSL[A]]]
    extends DataSourceReadSupport[Reader, Decoder, Config, DSReader, ReadDSL]

trait DataSourceJavaIOWriteSupport[
  Encoder[_],
  Config[_],
  DSWriter <: DataSourceWriter[Writer, Encoder, Config],
  WriteDSL[A] <: DataSourceWriteDsl[A, Writer, Encoder, Config, DSWriter, WriteDSL[A]]]
    extends DataSourceWriteSupport[Writer, Encoder, Config, DSWriter, WriteDSL]

trait DataSourceJavaIOReader[Decoder[_], Config[_]] extends DataSourceReader[Reader, Decoder, Config]

trait DataSourceJavaIOReadDslMixin[
  A,
  Decoder[_],
  Config[_],
  DSReader <: DataSourceReader[Reader, Decoder, Config],
  Self <: DataSourceReadDsl[A, Reader, Decoder, Config, DSReader, Self]] {

  self: Self =>

  def load(content: String)(implicit ev: Decoder[A]): Iterable[A] =
    load(new StringReader(content))
}

trait DataSourceJavaIOWriteDslMixin[
  A,
  Encoder[_],
  Config[_],
  DSWriter <: DataSourceWriter[Writer, Encoder, Config],
  Self <: DataSourceWriteDsl[A, Writer, Encoder, Config, DSWriter, Self]] {

  self: Self =>

  def string(builder: mutable.StringBuilder)(implicit ev: Encoder[A]): SideEffect[Writer, ?, A] =
    save(new StringWriter)
      .afterAll { (writer, _) =>
        val content = writer.asInstanceOf[StringWriter].getBuffer.toString
        builder.append(content)
      }
}

trait DataSourceJavaIOWriter[Encoder[_], Config[_]] extends DataSourceWriter[Writer, Encoder, Config]

trait DataSourceJavaIOReaderFilesSupport[Decoder[_], Config[_]]
    extends DataSourceReaderFilesSupport[Reader, Decoder, Config] {
  this: DataSourceReader[Reader, Decoder, Config] =>

  override def openFile(path: Path, encoding: Charset): Reader =
    Files.newBufferedReader(path, encoding)
}

trait DataSourceJavaIOWriterFilesSupport[Encoder[_], Config[_]]
    extends DataSourceWriterFilesSupport[Writer, Encoder, Config] {
  this: DataSourceWriter[Writer, Encoder, Config] =>

  override def openFile(path: Path, encoding: Charset, openOptions: OpenOption*): Writer =
    Files.newBufferedWriter(path, encoding, openOptions: _*)

}
