package scalaql.excel

import scala.deriving.Mirror
import magnolia1.*
import org.apache.poi.ss.usermodel.Row

trait ExcelRowEncoderAutoDerivation extends ProductDerivation[ExcelEncoder] {

  def join[T](ctx: CaseClass[ExcelEncoder, T]): ExcelEncoder[T] = new ExcelEncoder[T] {
    override def headers: List[String] =
      ctx.params.toList.flatMap { param =>
        val nestedHeaders = param.typeclass.headers
        if (nestedHeaders.isEmpty) List(param.label)
        else nestedHeaders
      }

    override def write(value: T, into: ExcelTableApi)(implicit writeContext: ExcelWriteContext): WriteResult = {
      val cellsWritten = ctx.params.foldLeft(0) { (cellsWritten, param) =>
        cellsWritten + param.typeclass
          .write(param.deref(value), into)(
            writeContext
              .enterField(param.label)
              .copy(
                startOffset = writeContext.startOffset + cellsWritten
              )
          )
          .cellsWritten
      }
      WriteResult(cellsWritten)
    }
  }

  inline given autoDerive[T](using Mirror.Of[T]): ExcelEncoder[T] = derived[T]
}
