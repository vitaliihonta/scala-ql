package scalaql.csv

import language.experimental.macros
import magnolia1.*

trait CsvEncoderAutoDerivation {
  type Typeclass[T] = CsvEncoder[T]

  def join[T](ctx: CaseClass[CsvEncoder, T]): CsvEncoder[T] = new CsvEncoder[T] {
    override def headers: List[String] = ctx.parameters.toList.flatMap { param =>
      val nestedHeaders = param.typeclass.headers
      if (nestedHeaders.isEmpty) List(param.label)
      else nestedHeaders
    }

    override def write(value: T)(implicit writeContext: CsvContext): CsvEncoder.Result =
      ctx.parameters.flatMap { param =>
        param.typeclass
          .write(param.dereference(value))(
            writeContext.copy(
              path = param.label :: writeContext.path
            )
          )
      }.toMap
  }

  implicit def autoDerive[T]: CsvEncoder[T] = macro Magnolia.gen[T]
}
