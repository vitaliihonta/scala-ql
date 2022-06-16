package scalaql.json

case class JsonConfig(multiline: Boolean, lineTerminator: String)

object JsonConfig extends LowPriorityJsonConfig

trait LowPriorityJsonConfig {
  implicit val default: JsonConfig = JsonConfig(
    multiline = true,
    lineTerminator = "\r\n"
  )
}
