package scalaql.sources

import scalaql.SideEffect
import scala.collection.mutable

trait DataSourceSupport[Source <: AutoCloseable, Sink, Decoder[_], Encoder[_], ReadConfig, WriteConfig]
    extends DataSourceReadSupport[Source, Decoder, ReadConfig]
    with DataSourceWriteSupport[Sink, Encoder, WriteConfig]

trait DataSourceReadSupport[Source <: AutoCloseable, Decoder[_], Config] {
  val read: DataSourceReader[Source, Decoder, Config]
}

trait DataSourceWriteSupport[Sink, Encoder[_], Config] {
  val write: DataSourceWriter[Sink, Encoder, Config]
}

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

trait DataSourceWriter[Sink, Encoder[_], Config] {
  def write[A: Encoder](
    sink:            => Sink
  )(implicit config: Config
  ): SideEffect[?, ?, A]

  def string[A: Encoder](
    builder:         mutable.StringBuilder
  )(implicit config: Config
  ): SideEffect[?, ?, A]
}
