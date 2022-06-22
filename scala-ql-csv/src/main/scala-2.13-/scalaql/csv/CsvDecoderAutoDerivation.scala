package scalaql.csv

import language.experimental.macros
import magnolia1.*

trait CsvDecoderAutoDerivation {
  type Typeclass[T] = CsvDecoder[T]

  def join[T](ctx: CaseClass[CsvDecoder, T]): CsvDecoder[T] = new CsvDecoder[T] {
    override def read(row: Map[String, String])(implicit readerContext: CsvReadContext): CsvDecoder.Result[T] =
      ctx
        .constructEither { param =>
          param.typeclass
            .read(row)(
              readerContext.enterField(param.label)
            )
        }
        .left
        .map(readerContext.accumulatingError(s"${ctx.typeName.short} (at `${readerContext.location}`)", _))
  }

  implicit def autoDerive[T]: CsvDecoder[T] = macro Magnolia.gen[T]
}
