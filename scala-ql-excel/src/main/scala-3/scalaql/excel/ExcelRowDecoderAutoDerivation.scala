package scalaql.excel

import scala.deriving.Mirror
import magnolia1.*
import org.apache.poi.ss.usermodel.Row

trait ExcelRowDecoderAutoDerivation extends ProductDerivation[ExcelDecoder] {

  def join[T](ctx: CaseClass[ExcelDecoder, T]): ExcelDecoder[T] = new ExcelDecoder[T] {

    override def read(row: Row, offset: Int)(implicit readerContext: ReaderContext): T = {
      val values = ctx.params.zipWithIndex.map { case (param, idx) =>
        param.typeclass.read(
          row,
          readerContext.cellResolutionStrategy.getStartOffset(
            readerContext.headers,
            param.label,
            offset + idx
          )
        )
      }
      ctx.rawConstruct(values)
    }
  }

  inline given autoDerive[T](using Mirror.Of[T]): ExcelDecoder[T] = derived[T]
}
