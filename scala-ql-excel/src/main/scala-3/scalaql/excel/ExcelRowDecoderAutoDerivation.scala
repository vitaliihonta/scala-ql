package scalaql.excel

import scala.deriving.Mirror
import magnolia1.*
import org.apache.poi.ss.usermodel.Row

trait ExcelRowDecoderAutoDerivation extends ProductDerivation[ExcelDecoder] {

  def join[T](ctx: CaseClass[ExcelDecoder, T]): ExcelDecoder[T] = new ExcelDecoder[T] {

    override def read(row: Row)(implicit readerContext: ReaderContext): ReadResult[T] = {
      val (values, readTotal) =
        ctx.params.foldLeft(Seq.empty[Any] -> 0) { case ((fields, readCells), param) =>
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

  inline given autoDerive[T](using Mirror.Of[T]): ExcelDecoder[T] = derived[T]
}
