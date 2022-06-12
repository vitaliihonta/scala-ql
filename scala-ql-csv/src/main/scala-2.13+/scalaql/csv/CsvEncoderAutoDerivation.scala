package scalaql.csv

import language.experimental.macros
import magnolia1.*

trait CsvEncoderAutoDerivation {
  type Typeclass[T] = CsvEncoder[T]

  def join[T](ctx: CaseClass[CsvEncoder, T]): CsvEncoder.Row[T] = new CsvEncoder.Row[T] {
    override def write(value: T): CsvEntry.Row =
      CsvEntry.Row {
        ctx.parameters.foldLeft(Map.empty[String, String]) { (row, param) =>
          val written = param.typeclass.write(param.dereference(value))
          val field = written match {
            case e: CsvEntry.Field => e.field
            case _ =>
              throw new IllegalArgumentException(s"CsvEncoder doesn't support nested csv for field ${param.label}")
          }
          row + (param.label -> field)
        }
      }
  }

  implicit def autoDerive[T]: CsvEncoder.Row[T] = macro Magnolia.gen[T]
}
