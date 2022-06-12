package scalaql.csv

import scala.deriving.Mirror
import magnolia1.*

trait CsvEncoderAutoDerivation extends ProductDerivation[CsvEncoder] {

  def join[T](ctx: CaseClass[CsvEncoder, T]): CsvEncoder.Row[T] = new CsvEncoder.Row[T] {
    override def write(value: T): CsvEntry.Row =
      CsvEntry.Row {
        ctx.params.foldLeft(Map.empty[String, String]) { (row, param) =>
          val written = param.typeclass.write(param.deref(value))
          val field = written match {
            case e: CsvEntry.Field => e.field
            case _ =>
              throw new IllegalArgumentException(s"CsvEncoder doesn't support nested csv for field ${param.label}")
          }
          row + (param.label -> field)
        }
      }
  }

  inline given autoDerive[T](using Mirror.Of[T]): CsvEncoder.Row[T] = derived[T].asInstanceOf[CsvEncoder.Row[T]]
}
