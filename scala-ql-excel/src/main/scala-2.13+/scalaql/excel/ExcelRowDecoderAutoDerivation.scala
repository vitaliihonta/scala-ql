package scalaql.excel

import language.experimental.macros
import magnolia1.*
import org.apache.poi.ss.usermodel.Row

trait ExcelRowDecoderAutoDerivation {
  type Typeclass[T] = ExcelDecoder[T]

  def join[T](ctx: CaseClass[ExcelDecoder, T]): ExcelDecoder[T] = new ExcelDecoder[T] {

    override def read(row: Row, offset: Int)(implicit readerContext: ReaderContext): T = {
      val values = ctx.parameters.zipWithIndex.map { case (param, idx) =>
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

  implicit def autoDerive[T]: ExcelDecoder[T] = macro Magnolia.gen[T]
}
