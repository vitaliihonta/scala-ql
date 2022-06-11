package scalaql.json

case class JsonConfig(multiline: Boolean)

object JsonConfig {

  val default: JsonConfig = JsonConfig(
    multiline = true
  )
}
