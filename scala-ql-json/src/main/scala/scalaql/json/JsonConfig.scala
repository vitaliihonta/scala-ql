package scalaql.json

case class JsonReadConfig(multiline: Boolean, lineTerminator: String)

object JsonReadConfig {
  val default: JsonReadConfig = JsonReadConfig(
    multiline = true,
    lineTerminator = "\r\n"
  )
}

case class JsonWriteConfig(multiline: Boolean, lineTerminator: String)

object JsonWriteConfig {
  val default: JsonWriteConfig = JsonWriteConfig(
    multiline = true,
    lineTerminator = "\r\n"
  )
}
