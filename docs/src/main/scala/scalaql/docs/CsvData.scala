package scalaql.docs

object CsvData {
  case class EnterpriseSurvey(
    year:               Int,
    industryCodeAnzsic: String,
    industryNameAnzsic: String,
    rmeSizeGrp:         String,
    variable:           String,
    value:              String,
    unit:               String)
}
