package scalaql.visualization

import scala.deriving.Mirror
import magnolia1.*

trait ShowAsTableDerivation extends ProductDerivation[ShowAsTable] {

  def join[T](ctx: CaseClass[ShowAsTable, T]): ShowAsTable[T] = new ShowAsTable[T] {
    override def toRow(value: T): Map[String, String] =
      ctx.params.foldLeft(Map.empty[String, String]) { (acc, param) =>
        val writtenField = param.typeclass.toRow(param.deref(value))
        if (writtenField.size == 1 && writtenField.contains(ShowAsTable.SingleFieldKey)) {
          acc + (param.label -> writtenField(ShowAsTable.SingleFieldKey))
        } else {
          acc ++ writtenField.map { case (k, v) => s"${param.label}.$k" -> v }
        }
      }
  }

  inline given autoDerive[T](using Mirror.Of[T]): ShowAsTable[T] = derived[T]
}
