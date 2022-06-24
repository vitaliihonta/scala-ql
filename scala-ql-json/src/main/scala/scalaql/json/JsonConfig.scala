package scalaql.json

case class JsonReadConfig(multiline: Boolean, lineTerminator: String)

object JsonReadConfig extends LowPriorityJsonReadConfig {}

trait LowPriorityJsonReadConfig {
  implicit val default: JsonReadConfig = JsonReadConfig(
    multiline = true,
    lineTerminator = "\r\n"
  )
}

case class JsonWriteConfig(multiline: Boolean, lineTerminator: String)

object JsonWriteConfig extends LowPriorityJsonWriteConfig {}

trait LowPriorityJsonWriteConfig {
  implicit val default: JsonWriteConfig = JsonWriteConfig(
    multiline = true,
    lineTerminator = "\r\n"
  )
}
