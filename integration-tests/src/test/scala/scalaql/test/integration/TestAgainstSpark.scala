package scalaql.test.integration

import org.scalatest.BeforeAndAfterAll
import org.apache.spark.sql.*
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.*
import org.apache.spark.sql.types.DoubleType

import java.nio.file.Paths
import scalaql.*

object TestAgainstSpark {

  case class EnterpriseSurvey(
    year:                 Int,
    industry_code_ANZSIC: String,
    industry_name_ANZSIC: String,
    rme_size_grp:         String,
    variable:             String,
    value:                String,
    unit:                 String) {

    def decimalValue: Double =
      value.toDoubleOption.getOrElse(0.0)
  }

  case class SurveyStats(
    year:                 Int,
    industry_name_ANZSIC: String,
    total_profit:         Double,
    avg_profit:           Double)

  case class EnterpriseSurveyWindow(
    year:                 Int,
    industry_code_ANZSIC: String,
    industry_name_ANZSIC: String,
    rme_size_grp:         String,
    variable:             String,
    value:                String,
    unit:                 String,
    avg_value:            Double)
}

class TestAgainstSpark extends ScalaqlUnitSpec with BeforeAndAfterAll {
  import TestAgainstSpark.*

  private lazy val spark: SparkSession = SparkSession
    .builder()
    .master("local[4]")
    .appName("test")
    .getOrCreate()

  import spark.implicits.*

  override def beforeAll(): Unit = {
    val _ = spark
  }
  override def afterAll(): Unit =
    spark.close()

  private val surveyDir = Paths.get("docs/src/main/resources/annual-enterprise-survey-2020/")

  "scalaql" should {

    "do groupBy + aggregate the same as spark" in {
      println(s"Current path: ${Paths.get(".").toAbsolutePath}")

      val actualResult = select[EnterpriseSurvey]
        .where(_.year >= 2015)
        .groupBy(_.year, _.industry_name_ANZSIC)
        .aggregate { case ((year, industry), survey) =>
          (
            survey.sumBy(_.decimalValue) &&
              survey.avgBy(_.decimalValue)
          ).map { case (total, avg) => SurveyStats(year, industry, total, avg) }
        }
        .toList
        .run(readSurvey())

      val expectedResult = readSurveyBySpark()
        .where($"year" >= 2015)
        .groupBy($"year", $"industry_name_ANZSIC")
        .agg(
          sum($"decimal_value").as("total_profit"),
          avg($"decimal_value").as("avg_profit")
        )
        .as[SurveyStats]
        .collect()
        .toList

      actualResult should contain theSameElementsAs {
        expectedResult
      }
    }

    "do windowing as spark" in {

      val expectedResult = readSurveyBySpark()
        .where($"year" >= 2019)
        .withColumn(
          "avg_value",
          max($"decimal_value").over(
            Window.partitionBy($"industry_code_ANZSIC").orderBy($"year")
          )
        )
        .orderBy($"industry_code_ANZSIC", $"year")
        .show(truncate=false)
//        .as[EnterpriseSurveyWindow]
//        .collect()
//        .toList

      val actualResult = select[EnterpriseSurvey]
        .where(_.year >= 2019)
        .window(
          _.maxOf(_.decimalValue)
        )
        .over(
          _.partitionBy(_.industry_code_ANZSIC)
            .orderBy(_.year)
        )
        .map { case (survey, avgValue) =>
          EnterpriseSurveyWindow(
            year = survey.year,
            industry_code_ANZSIC = survey.industry_code_ANZSIC,
            industry_name_ANZSIC = survey.industry_name_ANZSIC,
            rme_size_grp = survey.rme_size_grp,
            variable = survey.variable,
            value = survey.value,
            unit = survey.unit,
            avg_value = avgValue
          )
        }
        .sortBy(e => e.industry_code_ANZSIC -> e.year)
        .show(truncate=false)
//        .toList
        .run(readSurvey())

//      actualResult should contain theSameElementsAs {
//        expectedResult
//      }
    }
  }

  private def readSurvey() =
    from(
      csv
        .read[EnterpriseSurvey]
        .directory(surveyDir, globPattern = "**/*.csv")
    )

  private def readSurveyBySpark(): DataFrame =
    spark.read
      .schema(implicitly[Encoder[EnterpriseSurvey]].schema)
      .csv(s"$surveyDir/*")
      .withColumn(
        "decimal_value",
        coalesce(
          $"value".cast(DoubleType),
          lit(0.0)
        )
      )
}
