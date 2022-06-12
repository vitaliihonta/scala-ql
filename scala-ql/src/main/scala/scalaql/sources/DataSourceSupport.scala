package scalaql.sources

import scalaql.SideEffectWithResource
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.OpenOption
import java.nio.file.Path
import scala.collection.mutable

trait DataSourceSupport[Decoder[_], Encoder[_], Config] {
  val read: DataSourceReadSupport[Decoder, Config]
  val write: DataSourceWriteSupport[Encoder, Config]
}

trait DataSourceReadSupport[Decoder[_], Config] {
  def file[A: Decoder](
    path:            Path
  )(implicit config: Config
  ): Iterable[A] = file(path, encoding = StandardCharsets.UTF_8)

  def file[A: Decoder](
    path:            Path,
    encoding:        Charset
  )(implicit config: Config
  ): Iterable[A]

  def string[A: Decoder](
    content:         String
  )(implicit config: Config
  ): Iterable[A]
}

trait DataSourceWriteSupport[Encoder[_], Config] {
  def file[A: Encoder](
    path:            Path
  )(implicit config: Config
  ): SideEffectWithResource[?, ?, A] =
    file(path, encoding = StandardCharsets.UTF_8)

  def file[A: Encoder](
    path:            Path,
    encoding:        Charset,
    openOptions:     OpenOption*
  )(implicit config: Config
  ): SideEffectWithResource[?, ?, A]

  def string[A: Encoder](
    builder:         mutable.StringBuilder
  )(implicit config: Config
  ): SideEffectWithResource[?, ?, A]
}
