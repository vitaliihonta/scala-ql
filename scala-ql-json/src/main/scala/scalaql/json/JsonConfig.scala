package scalaql.json

case class JsonConfig(multiline: Boolean)

object JsonConfig extends LowPriorityJsonConfig

trait LowPriorityJsonConfig {
  implicit val default: JsonConfig = JsonConfig(
    multiline = true
  )
}
