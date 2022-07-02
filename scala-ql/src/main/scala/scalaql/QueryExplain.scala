package scalaql

import scala.collection.mutable

sealed trait QueryExplain {
  override def toString: String =
    QueryExplain.render(this)
}

object QueryExplain {
  case class Single(query: String) extends QueryExplain

  case class Continuation(source: QueryExplain, next: QueryExplain) extends QueryExplain

  case class Operation(left: QueryExplain, right: QueryExplain, op: String) extends QueryExplain

  type OnDepth[A] = Int => A
  object OnDepth {
    def const[A](value: A): OnDepth[A] = _ => value
  }

  case class Config(
    sep:           OnDepth[String],
    ident:         OnDepth[String],
    beforeLeftOp:  OnDepth[String],
    afterLeftOp:   OnDepth[String],
    beforeRightOp: OnDepth[String],
    afterRightOp:  OnDepth[String])

  val defaultConfig: Config = Config(
    sep = OnDepth.const(" -> "),
    ident = OnDepth.const(" "),
    beforeLeftOp = OnDepth.const(""),
    afterLeftOp = OnDepth.const(""),
    beforeRightOp = OnDepth.const(""),
    afterRightOp = OnDepth.const("")
  )

  // TODO: add correct pretty print
  def render(explain: QueryExplain, config: Config = defaultConfig): String = {
    val sb = new mutable.StringBuilder
    def go(current: QueryExplain, depth: Int): Unit = current match {
      case Single(query) => sb.append(query)
      case Continuation(source, next) =>
        go(source, depth + 1)
        sb.append(config.sep(depth))
        go(next, depth + 1)
      case Operation(left, right, op) =>
        sb.append(config.beforeLeftOp(depth))
        go(left, depth + 1)
        sb.append(config.afterLeftOp(depth))
          .append(config.ident(depth))
          .append(op)
          .append(config.ident(depth))
          .append(config.beforeRightOp(depth))
        go(right, depth + 1)
        sb.append(config.afterRightOp(depth))
    }
    go(explain, depth = 0)
    sb.toString
  }
}
