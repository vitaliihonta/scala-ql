package scalaql.excel

import language.experimental.macros
import magnolia1.*
import org.apache.poi.ss.usermodel.Row

trait ExcelRowEncoderAutoDerivation {
  type Typeclass[T] = ExcelEncoder[T]

  def join[T](ctx: CaseClass[ExcelEncoder, T]): ExcelEncoder[T] = new ExcelEncoder[T] {
    override def headers: List[String] =
      ctx.parameters.toList.flatMap { param =>
        val nestedHeaders = param.typeclass.headers
        if (nestedHeaders.isEmpty) List(param.label)
        else nestedHeaders
      }

    override def write(row: Row, value: T)(implicit writeContext: ExcelWriteContext): WriteResult = {
      val cellsWritten = ctx.parameters.foldLeft(0) { (cellsWritten, param) =>
        cellsWritten + param.typeclass
          .write(row, param.dereference(value))(
            writeContext.copy(
              path = param.label :: writeContext.path,
              startOffset = writeContext.startOffset + cellsWritten
            )
          )
          .cellsWritten
      }
      WriteResult(cellsWritten)
    }
  }

  implicit def autoDerive[T]: ExcelEncoder[T] = macro Magnolia.gen[T]
}
