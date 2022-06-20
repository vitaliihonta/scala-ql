package scalaql.csv

import scala.deriving.Mirror
import magnolia1.*

trait CsvDecoderAutoDerivation extends ProductDerivation[CsvDecoder] {

  def join[T](ctx: CaseClass[CsvDecoder, T]): CsvDecoder[T] = new CsvDecoder[T] {
    override def read(row: Map[String, String])(implicit readerContext: CsvContext): ReadResult[T] = {
      val (values, readTotal) =
        ctx.params.foldLeft(Seq.empty[Any] -> 0) { case ((fields, readCells), param) =>
          val ReadResult(result, read) = param.typeclass.read(row)(
            readerContext.copy(
              path = param.label :: readerContext.path
            )
          )
          (fields :+ result, readCells + read)
        }

      ReadResult(ctx.rawConstruct(values), readTotal)
    }
  }

  inline given autoDerive[T](using Mirror.Of[T]): CsvDecoder[T] = derived[T]
}
