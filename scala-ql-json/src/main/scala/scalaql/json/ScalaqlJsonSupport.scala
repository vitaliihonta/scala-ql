package scalaql.json

import io.circe.{Decoder, Encoder}
import scalaql.json.internal.{JsonDataSourceReader, JsonDataSourceWriter}
import scalaql.sources.*

trait ScalaqlJsonSupport
    extends DataSourceJavaIOSupport[
      Decoder,
      Encoder,
      λ[a => JsonReadConfig],
      λ[a => JsonWriteConfig],
      JsonDataSourceReader,
      JsonDataSourceWriter,
      JsonReadDsl,
      JsonWriteDsl
    ] {

  override def read[A]: JsonReadDsl[A]   = new JsonReadDsl[A](JsonReadConfig.default)
  override def write[A]: JsonWriteDsl[A] = new JsonWriteDsl[A](JsonWriteConfig.default)

}
