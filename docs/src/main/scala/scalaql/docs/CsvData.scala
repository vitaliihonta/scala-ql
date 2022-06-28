package scalaql.docs

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileSystem, FileSystems, Files, Path}

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

object Test extends App {
  import scalaql._
  import scalaql.csv.CsvReadConfig
  import scalaql.sources.Naming

  // Docs classes
  import scalaql.docs.CsvData._
  import scalaql.docs.DocUtils._

  // Imports for examples
  import java.nio.file.Paths

  implicit val csvConfig: CsvReadConfig = CsvReadConfig.default.copy(naming = Naming.SnakeCase)

  val dir = Paths.get("docs/src/main/resources/annual-enterprise-survey-2020/")

  select[EnterpriseSurvey]
    .where(_.year > 2015)
    .show(truncate = false)
    .run(
      from(
        csv.read.directory[EnterpriseSurvey](dir, globPattern = "**/*.csv")
      )
    )
}
