package scalaql.csv

import language.experimental.macros
import magnolia1.*

trait CsvDecoderAutoDerivation {
  type Typeclass[T] = CsvDecoder[T]

  def join[T](ctx: CaseClass[CsvDecoder, T]): CsvDecoder[T] = new CsvDecoder[T] {
    override def read(row: Map[String, String])(implicit readerContext: CsvContext): ReadResult[T] = {
      val (values, readTotal) =
        ctx.parameters.foldLeft(Seq.empty[Any] -> 0) { case ((fields, readCells), param) =>
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

  implicit def autoDerive[T]: CsvDecoder[T] = macro Magnolia.gen[T]
}
