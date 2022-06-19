package scalaql.sources

import scalaql.SideEffect
import scala.collection.mutable

trait DataSourceSupport[Source <: AutoCloseable, Sink, Decoder[_], Encoder[_], ReadConfig[_], WriteConfig[_]]
    extends DataSourceReadSupport[Source, Decoder, ReadConfig]
    with DataSourceWriteSupport[Sink, Encoder, WriteConfig]

trait DataSourceReadSupport[Source <: AutoCloseable, Decoder[_], Config[_]] {
  val read: DataSourceReader[Source, Decoder, Config]
}

trait DataSourceWriteSupport[Sink, Encoder[_], Config[_]] {
  val write: DataSourceWriter[Sink, Encoder, Config]
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

trait DataSourceWriter[Sink, Encoder[_], Config[_]] {
  def write[A: Encoder](
    sink:            => Sink
  )(implicit config: Config[A]
  ): SideEffect[?, ?, A]

  def string[A: Encoder](
    builder:         mutable.StringBuilder
  )(implicit config: Config[A]
  ): SideEffect[?, ?, A]
}
