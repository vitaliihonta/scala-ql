package scalaql.visualization

import language.experimental.macros
import magnolia1.*

trait ShowAsTableDerivation {
  type Typeclass[T] = ShowAsTable[T]

  def join[T](ctx: CaseClass[ShowAsTable, T]): ShowAsTable[T] = new Typeclass[T] {
    override def toRow(value: T): Map[String, String] =
      ctx.parameters.foldLeft(Map.empty[String, String]) { (acc, param) =>
        val writtenField = param.typeclass.toRow(param.dereference(value))
        if (writtenField.size == 1 && writtenField.contains(ShowAsTable.SingleFieldKey)) {
          acc + (param.label -> writtenField(ShowAsTable.SingleFieldKey))
        } else {
          acc ++ writtenField.map { case (k, v) => s"${param.label}.$k" -> v }
        }
      }
  }

  implicit def autoDerive[T]: ShowAsTable[T] = macro Magnolia.gen[T]
}
