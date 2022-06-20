package scalaql.csv

import language.experimental.macros
import magnolia1.*

trait CsvEncoderAutoDerivation {
  type Typeclass[T] = CsvEncoder[T]

  def join[T](ctx: CaseClass[CsvEncoder, T]): CsvEncoder[T] = new CsvEncoder[T] {
    override def headers: List[String] = ctx.parameters.toList.flatMap { param =>
      val nestedHeaders = param.typeclass.headers
      if (nestedHeaders.isEmpty) List(param.label)
      else nestedHeaders
    }

    override def write(value: T)(implicit writeContext: CsvContext): WriteResult = {
      val (resultRow, totalWritten) = ctx.parameters.foldLeft(Map.empty[String, String] -> 0) {
        case ((baseRow, cellsWritten), param) =>
          val WriteResult(row, written) = param.typeclass
            .write(param.dereference(value))(
              writeContext.copy(
                path = param.label :: writeContext.path
              )
            )

          (baseRow ++ row, cellsWritten + written)
      }
      WriteResult(resultRow, totalWritten)
    }
  }

  implicit def autoDerive[T]: CsvEncoder[T] = macro Magnolia.gen[T]
}
