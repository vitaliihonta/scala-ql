package scalaql.csv

import scala.deriving.Mirror
import magnolia1.*

trait CsvEncoderAutoDerivation extends ProductDerivation[CsvEncoder] {

  def join[T](ctx: CaseClass[CsvEncoder, T]): CsvEncoder[T] = new CsvEncoder[T] {
    override def headers: List[String] = ctx.params.toList.flatMap { param =>
      val nestedHeaders = param.typeclass.headers
      if (nestedHeaders.isEmpty) List(param.label)
      else nestedHeaders
    }

    override def write(value: T)(implicit writeContext: CsvContext): WriteResult = {
      val (resultRow, totalWritten) = ctx.params.foldLeft(Map.empty[String, String] -> 0) {
        case ((baseRow, cellsWritten), param) =>
          val WriteResult(row, written) = param.typeclass
            .write(param.deref(value))(
              writeContext.copy(
                path = param.label :: writeContext.path
              )
            )

          (baseRow ++ row, cellsWritten + written)
      }
      WriteResult(resultRow, totalWritten)
    }
  }

  inline given autoDerive[T](using Mirror.Of[T]): CsvEncoder[T] = derived[T]
}
