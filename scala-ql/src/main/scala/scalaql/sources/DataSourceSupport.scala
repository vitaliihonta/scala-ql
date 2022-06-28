package scalaql.sources

import scalaql.SideEffect

trait DataSourceSupport[
  Source <: AutoCloseable,
  Sink,
  Decoder[_],
  Encoder[_],
  ReadConfig[_],
  WriteConfig[_],
  DSReader <: DataSourceReader[Source, Decoder, ReadConfig],
  DSWriter <: DataSourceWriter[Sink, Encoder, WriteConfig],
  ReadDSL[A] <: DataSourceReadDsl[A, Source, Decoder, ReadConfig, DSReader, ReadDSL[A]],
  WriteDSL[A] <: DataSourceWriteDsl[A, Sink, Encoder, WriteConfig, DSWriter, WriteDSL[A]]]
    extends DataSourceReadSupport[Source, Decoder, ReadConfig, DSReader, ReadDSL]
    with DataSourceWriteSupport[Sink, Encoder, WriteConfig, DSWriter, WriteDSL]

trait DataSourceReadSupport[
  Source <: AutoCloseable,
  Decoder[_],
  Config[_],
  DSReader <: DataSourceReader[Source, Decoder, Config],
  DSL[A] <: DataSourceReadDsl[A, Source, Decoder, Config, DSReader, DSL[A]]] {

  def read[A]: DSL[A]
}

trait DataSourceWriteSupport[
  Sink,
  Encoder[_],
  Config[_],
  DSWriter <: DataSourceWriter[Sink, Encoder, Config],
  DSL[A] <: DataSourceWriteDsl[A, Sink, Encoder, Config, DSWriter, DSL[A]]] {

  def write[A]: DSL[A]
}

abstract class DataSourceReadDsl[
  A,
  Source <: AutoCloseable,
  Decoder[_],
  Config[_],
  DSReader <: DataSourceReader[Source, Decoder, Config],
  Self <: DataSourceReadDsl[A, Source, Decoder, Config, DSReader, Self]] {

  protected def _reader: DSReader

  def config: Config[A]

  def withConfig(config: Config[A]): Self

  def load(source: => Source)(implicit ev: Decoder[A]): Iterable[A] =
    _reader.read[A](source)(ev, config)
}

trait DataSourceReader[Source <: AutoCloseable, Decoder[_], Config[_]] {

  /** Implement reading logic here. NOTE - no need to close the reader, it's handled in public methods
    */
  protected def readImpl[A: Decoder](source: Source)(implicit config: Config[A]): Iterable[A]

  def read[A: Decoder](
    source:          => Source
  )(implicit config: Config[A]
  ): Iterable[A] = {
    val theSource = source
    try readImpl[A](theSource)
    finally theSource.close()
  }
}

abstract class DataSourceWriteDsl[
  A,
  Sink,
  Encoder[_],
  Config[_],
  DSWriter <: DataSourceWriter[Sink, Encoder, Config],
  Self <: DataSourceWriteDsl[A, Sink, Encoder, Config, DSWriter, Self]] {

  protected def _writer: DSWriter

  def config: Config[A]

  def withConfig(config: Config[A]): Self

  def save(sink: => Sink)(implicit ev: Encoder[A]): SideEffect[Sink, ?, A] =
    _writer.write[A](sink)(ev, config)
}

trait DataSourceWriter[Sink, Encoder[_], Config[_]] {
  def write[A: Encoder](
    sink:            => Sink
  )(implicit config: Config[A]
  ): SideEffect[Sink, ?, A]
}
