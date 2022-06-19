package scalaql.json

case class JsonConfig(multiline: Boolean, lineTerminator: String)

object JsonConfig extends LowPriorityJsonConfig {
  type Adapt[A] = JsonConfig
}

trait LowPriorityJsonConfig {
  implicit val default: JsonConfig = JsonConfig(
    multiline = true,
    lineTerminator = "\r\n"
  )
}
