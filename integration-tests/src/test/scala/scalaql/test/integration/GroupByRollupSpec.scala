package scalaql.test.integration

import org.scalactic.Equality
import scalaql.*
import scalaql.csv.CsvDecoder
import java.nio.file.Paths
import java.time.LocalDate

object GroupByRollupSpec {
  case class OrderStatsRollup(
    customerId:  Option[String],
    shipCountry: Option[String],
    maxFreight:  Double,
    avgFreight:  Double)
}

class GroupByRollupSpec extends ScalaqlUnitSpec {

  import GroupByRollupSpec.*

  private implicit val statsEquality: Equality[OrderStatsRollup] =
    new Equality[OrderStatsRollup] {
      override def areEqual(a: OrderStatsRollup, b: Any): Boolean = b match {
        case b: OrderStatsRollup =>
          a.customerId == b.customerId && a.shipCountry == b.shipCountry &&
          (a.maxFreight === b.maxFreight +- 0.1) && (a.avgFreight === b.avgFreight +- 0.1)
        case _ => false
      }
    }

  "scalaql" should {
    "correctly process simple groupByRollup & aggregate" in {
      // SELECT "CustomerId", "ShipCountry", MAX("Freight") as "MaxFreight", AVG("Freight") as "AvgFreight"
      // FROM public."Order"
      //  where "ShipCountry"  in ('Poland', 'USA')
      //  group by rollup ("ShipCountry", "CustomerId")
      //  order by "CustomerId" nulls first, "ShipCountry"
      val query = select[Order]
        .where(_.shipCountry isIn ("Poland", "USA"))
        .groupByRollup(
          _.customerId,
          _.shipCountry
        )
        .aggregate(order => order.maxOf(_.freight) && order.avgBy(_.freight))
        .mapTo(OrderStatsRollup)
        .orderBy(_.customerId, _.shipCountry)

      val actualResult = query.toList
//        .show(truncate=false,numRows=200)
        .run(
          from(
            csv
              .read[Order]
              .option(Naming.Capitalize)
              .file(
                Paths.get("integration-tests/src/test/resources/input/_Order__202207251611.csv")
              )
          )
        )
      val expectedResult = Fixture.readExpectedResult[OrderStatsRollup]("simple_rollup")

      assert(actualResult.size == expectedResult.size)

      actualResult should contain theSameElementsAs expectedResult
    }
  }
}
