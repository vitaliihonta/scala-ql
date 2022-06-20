package scalaql.csv

import language.experimental.macros
import magnolia1.*

trait CsvDecoderAutoDerivation {
  type Typeclass[T] = CsvDecoder[T]

  def join[T](ctx: CaseClass[CsvDecoder, T]): CsvDecoder[T] = new CsvDecoder[T] {
    override def read(row: Map[String, String])(implicit readerContext: CsvContext): CsvDecoder.Result[T] =
      ctx
        .constructEither { param =>
          param.typeclass
            .read(row)(
              readerContext.copy(
                path = param.label :: readerContext.path
              )
            )
        }
        .left
        .map(new CsvDecoderAccumulatingException(_))
  }

  implicit def autoDerive[T]: CsvDecoder[T] = macro Magnolia.gen[T]
}
