package scalaql.csv

import scala.deriving.Mirror
import magnolia1.*

trait CsvEncoderAutoDerivation extends ProductDerivation[CsvEncoder] {

  def join[T](ctx: CaseClass[CsvEncoder, T]): CsvEncoder[T] = new CsvEncoder[T] {
    override def headers: List[String] = ctx.params.toList.flatMap { param =>
      val nestedHeaders = param.typeclass.headers
      if (nestedHeaders.isEmpty) List(param.label)
      else nestedHeaders
    }

    override def write(value: T)(implicit writeContext: CsvWriteContext): CsvEncoder.Result =
      ctx.params.flatMap { param =>
        param.typeclass
          .write(param.deref(value))(
            writeContext.enterField(param.label)
          )
      }.toMap
  }

  inline given autoDerive[T](using Mirror.Of[T]): CsvEncoder[T] = derived[T]
}
