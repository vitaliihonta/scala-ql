package scalaql.json.internal

import io.circe.{Decoder, parser}
import scalaql.json.JsonReadConfig
import scalaql.sources.{DataSourceJavaIOReader, DataSourceJavaIOReaderFilesSupport, DataSourceJavaIOReaderHttpSupport}

import scala.jdk.CollectionConverters.*
import java.io.{BufferedReader, Reader}
import java.util.stream.Collectors

object JsonDataSourceReader extends JsonDataSourceReader

class JsonDataSourceReader
    extends DataSourceJavaIOReader[Decoder, λ[a => JsonReadConfig]]
    with DataSourceJavaIOReaderFilesSupport[Decoder, λ[a => JsonReadConfig]]
    with DataSourceJavaIOReaderHttpSupport[Decoder, λ[a => JsonReadConfig]] {

  protected def readImpl[A: Decoder](reader: Reader)(implicit config: JsonReadConfig): Iterable[A] = {
    val bufferedReader = new BufferedReader(reader)
    if (config.multiline) {
      bufferedReader
        .lines()
        // java lambdas, need types
        .map[A]((line: String) => parser.decode[A](line).toTry.get)
        .collect(Collectors.toList[A])
        .asScala
        .toList
    } else {
      val content = bufferedReader
        .lines()
        .collect(Collectors.joining())

      parser.decode[List[A]](content).toTry.get
    }
  }
}
