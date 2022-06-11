package scalaql.csv

import language.experimental.macros
import magnolia1.*

trait CsvDecoderAutoDerivation {
  type Typeclass[T] = CsvDecoder[T]

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

  implicit def autoDerive[T]: CsvDecoder.Row[T] = macro Magnolia.gen[T]
}
