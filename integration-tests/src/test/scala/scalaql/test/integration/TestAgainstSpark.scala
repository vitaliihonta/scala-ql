package scalaql.test.integration

import org.scalatest.BeforeAndAfterAll
import org.apache.spark.sql.*
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
        .run(
          from(
            csv
              .read[EnterpriseSurvey]
              .directory(surveyDir, globPattern = "**/*.csv")
          )
        )

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
  }

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
