package scalaql.csv

import scala.deriving.Mirror
import magnolia1.*

trait CsvDecoderAutoDerivation extends ProductDerivation[CsvDecoder] {

  def join[T](ctx: CaseClass[CsvDecoder, T]): CsvDecoder.Row[T] = new CsvDecoder.Row[T] {
    override def readRow(value: CsvDecoderInput.Row): T =
      ctx.construct(param =>
        param.typeclass.read(
          CsvDecoderInput.Field(
            value.row.getOrElse(
              param.label,
              throw new IllegalArgumentException(s"Field not found in row: ${param.label}")
            )
          )
        )
      )
  }

  inline given autoDerive[T](using Mirror.Of[T]): CsvDecoder.Row[T] = derived[T].asInstanceOf[CsvDecoder.Row[T]]
}
