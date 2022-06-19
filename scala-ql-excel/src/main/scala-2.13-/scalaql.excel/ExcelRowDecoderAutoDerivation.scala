package scalaql.excel

import language.experimental.macros
import magnolia1.*
import org.apache.poi.ss.usermodel.Row

trait ExcelRowDecoderAutoDerivation {
  type Typeclass[T] = ExcelDecoder[T]

  def join[T](ctx: CaseClass[ExcelDecoder, T]): ExcelDecoder[T] = new ExcelDecoder[T] {

    override def read(row: Row)(implicit readerContext: ExcelReadContext): ReadResult[T] = {
      val (values, readTotal) =
        ctx.parameters.foldLeft(Seq.empty[Any] -> 0) { case ((fields, readCells), param) =>
          val ReadResult(result, read) = param.typeclass.read(row)(
            readerContext.copy(
              path = param.label :: readerContext.path,
              currentOffset = readerContext.currentOffset + readCells
            )
          )
          (fields :+ result, readCells + read)
        }

      ReadResult(ctx.rawConstruct(values), readTotal)
    }
  }

  implicit def autoDerive[T]: ExcelDecoder[T] = macro Magnolia.gen[T]
}
