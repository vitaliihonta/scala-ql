package scalaql.docs

import scala.util.Try

object CsvData extends App {
  case class EnterpriseSurvey(
    year:               Int,
    industryCodeAnzsic: String,
    industryNameAnzsic: String,
    rmeSizeGrp:         String,
    variable:           String,
    value:              String,
    unit:               String) {

    def decimalValue: BigDecimal =
      Try(BigDecimal(value)).toOption.getOrElse(BigDecimal(0))
  }
}
