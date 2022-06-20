package scalaql.csv

import scala.deriving.Mirror
import magnolia1.*

trait CsvDecoderAutoDerivation extends ProductDerivation[CsvDecoder] {

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
        .map(new CsvDecoderAccumulatingException(s"${ctx.typeInfo.short} (at ${readerContext.pathStr})", _))
  }

  inline given autoDerive[T](using Mirror.Of[T]): CsvDecoder[T] = derived[T]
}
