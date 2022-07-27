package scalaql.test.integration

import org.scalactic.Equality
import scalaql.*
import scalaql.csv.CsvDecoder
import java.nio.file.Paths
import java.time.LocalDate

object GroupBySpec {
  case class OrderStatsRollup(
    customerId:  Option[String],
    shipCountry: Option[String],
    maxFreight:  Double,
    avgFreight:  Double)

  case class OrderStatsRollupFillna(
    customerId:  String,
    shipCountry: String,
    maxFreight:  Double,
    avgFreight:  Double)

  case class OrderStatsCube(
    customerId:   Option[String],
    shipCountry:  Option[String],
    totalFreight: Double,
    avgFreight:   Double)

  case class OrderStatsCubePartial(
    customerId:   Option[String],
    shipCountry:  Option[String],
    employeeId:   Long,
    totalFreight: Double,
    avgFreight:   Double)

  case class OrderStatsRollupPartial(
    customerId:   String,
    shipCountry:  Option[String],
    employeeId:   Option[Long],
    totalFreight: Double,
    avgFreight:   Double)

  case class OrderStatsGSets(
    customerId:   Option[String],
    shipCountry:  Option[String],
    totalFreight: Double,
    avgFreight:   Double)
}

class GroupBySpec extends ScalaqlUnitSpec {

  import GroupBySpec.*

  private implicit val statsEquality: Equality[OrderStatsRollup] =
    new Equality[OrderStatsRollup] {
      override def areEqual(a: OrderStatsRollup, b: Any): Boolean = b match {
        case b: OrderStatsRollup =>
          a.customerId == b.customerId && a.shipCountry == b.shipCountry &&
          (a.maxFreight === b.maxFreight +- 0.1) && (a.avgFreight === b.avgFreight +- 0.1)
        case _ => false
      }
    }

  private implicit val statsFillnaEquality: Equality[OrderStatsRollupFillna] =
    new Equality[OrderStatsRollupFillna] {
      override def areEqual(a: OrderStatsRollupFillna, b: Any): Boolean = b match {
        case b: OrderStatsRollupFillna =>
          a.customerId == b.customerId && a.shipCountry == b.shipCountry &&
          (a.maxFreight === b.maxFreight +- 0.1) && (a.avgFreight === b.avgFreight +- 0.1)
        case _ => false
      }
    }

  private implicit val statsRollupEquality: Equality[OrderStatsRollupPartial] =
    new Equality[OrderStatsRollupPartial] {
      override def areEqual(a: OrderStatsRollupPartial, b: Any): Boolean = b match {
        case b: OrderStatsRollupPartial =>
          a.customerId == b.customerId && a.shipCountry == b.shipCountry && a.employeeId == b.employeeId &&
          (a.totalFreight === b.totalFreight +- 0.1) && (a.avgFreight === b.avgFreight +- 0.1)
        case _ => false
      }
    }

  private implicit val statsGSetsEquality: Equality[OrderStatsGSets] =
    new Equality[OrderStatsGSets] {
      override def areEqual(a: OrderStatsGSets, b: Any): Boolean = b match {
        case b: OrderStatsGSets =>
          a.customerId == b.customerId && a.shipCountry == b.shipCountry &&
          (a.totalFreight === b.totalFreight +- 0.1) && (a.avgFreight === b.avgFreight +- 0.1)
        case _ => false
      }
    }

  private implicit val statsCubeEquality: Equality[OrderStatsCube] =
    new Equality[OrderStatsCube] {
      override def areEqual(a: OrderStatsCube, b: Any): Boolean = b match {
        case b: OrderStatsCube =>
          a.customerId == b.customerId && a.shipCountry == b.shipCountry &&
          (a.totalFreight === b.totalFreight +- 0.1) && (a.avgFreight === b.avgFreight +- 0.1)
        case _ => false
      }
    }

  private implicit val statsCubePartialEquality: Equality[OrderStatsCubePartial] =
    new Equality[OrderStatsCubePartial] {
      override def areEqual(a: OrderStatsCubePartial, b: Any): Boolean = b match {
        case b: OrderStatsCubePartial =>
          a.customerId == b.customerId && a.shipCountry == b.shipCountry && a.employeeId == b.employeeId &&
          (a.totalFreight === b.totalFreight +- 0.1) && (a.avgFreight === b.avgFreight +- 0.1)
        case _ => false
      }
    }

  "scalaql" should {
    "correctly process simple groupBy with rollup & aggregate" in {
      // SELECT "CustomerId", "ShipCountry", MAX("Freight") as "MaxFreight", AVG("Freight") as "AvgFreight"
      // FROM public."Order"
      //  where "ShipCountry"  in ('Poland', 'USA')
      //  group by rollup ("ShipCountry", "CustomerId")
      //  order by "CustomerId" nulls first, "ShipCountry"
      val query = select[Order]
        .where(_.shipCountry isIn ("Poland", "USA"))
        .groupBy(
          _.customerId.rollup,
          _.shipCountry.rollup
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

    "correctly process simple groupBy with rollup with fillna" in {
      // SELECT
      //   coalesce("CustomerId", 'All customers') as "CustomerId",
      //   coalesce("ShipCountry", 'All countries') as "ShipCountry",
      //   MAX("Freight") as "MaxFreight",
      //   AVG("Freight") as "AvgFreight"
      // FROM public."Order"
      // where "ShipCountry"  in ('Poland', 'USA')
      // group by rollup ("ShipCountry", "CustomerId")
      // order by "CustomerId" nulls first, "ShipCountry"
      val query = select[Order]
        .where(_.shipCountry isIn ("Poland", "USA"))
        .groupBy(
          _.customerId.rollup.fillna("All customers"),
          _.shipCountry.rollup.fillna("All countries")
        )
        .aggregate(order => order.maxOf(_.freight) && order.avgBy(_.freight))
        .mapTo(OrderStatsRollupFillna)
        .orderBy(_.customerId, _.shipCountry)

      val actualResult = query.toList
//        .show(truncate = false, numRows = 200)
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
      val expectedResult = Fixture.readExpectedResult[OrderStatsRollupFillna]("rollup_fillna")

      assert(actualResult.size == expectedResult.size)

      actualResult should contain theSameElementsAs expectedResult
    }

    "correctly process partial groupBy rollup" in {
      // SELECT "CustomerId", "ShipCountry", "EmployeeId", SUM("Freight") as "TotalFreight", AVG("Freight") as "AvgFreight" FROM public."Order"
      //      where "ShipCountry"  in ('Poland', 'USA')
      //      group by rollup("ShipCountry"), rollup("EmployeeId"), "CustomerId"
      //      order by "CustomerId" nulls first, "ShipCountry", "EmployeeId" nulls first
      val query = select[Order]
        .where(_.shipCountry isIn ("Poland", "USA"))
        .groupBy(
          _.customerId,
          _.shipCountry.rollup,
          _.employeeId.rollup
        )
        .aggregate(order => order.sumBy(_.freight) && order.avgBy(_.freight))
        .mapTo(OrderStatsRollupPartial)
        .orderBy(_.customerId, _.shipCountry, _.employeeId)

      val actualResult = query.toList
//        .show(truncate = false, numRows = 200)
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
      val expectedResult = Fixture.readExpectedResult[OrderStatsRollupPartial]("partial_rollup")

      assert(actualResult.size == expectedResult.size)

      actualResult should contain theSameElementsAs expectedResult
    }

    "correctly process groupBy with multiple cube" in {
      // SELECT "CustomerId", "ShipCountry", SUM("Freight") as "TotalFreight", AVG("Freight") as "AvgFreight" FROM public."Order"
      //      where "ShipCountry"  in ('Poland', 'USA')
      //      group by
      //        cube ("CustomerId", "ShipCountry")
      //      order by "CustomerId" nulls first, "ShipCountry"
      val query = select[Order]
        .where(_.shipCountry isIn ("Poland", "USA"))
        .groupBy(
          _.customerId.cube,
          _.shipCountry.cube
        )
        .aggregate(order => order.sumBy(_.freight) && order.avgBy(_.freight))
        .mapTo(OrderStatsCube)
        .orderBy(_.customerId, _.shipCountry)

      val actualResult = query.toList
//        .show(truncate = false, numRows = 200)
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

      val expectedResult = Fixture.readExpectedResult[OrderStatsCube]("multiple_cube")

      assert(actualResult.size == expectedResult.size)

      actualResult should contain theSameElementsAs expectedResult
    }

    "correctly process groupBy with partial cube" in {
      // SELECT "CustomerId", "ShipCountry","EmployeeId", SUM("Freight") as "TotalFreight", AVG("Freight") as "AvgFreight" FROM public."Order"
      //      where "ShipCountry"  in ('Poland', 'USA')
      //      group by
      //        cube ("CustomerId", "ShipCountry"), "EmployeeId"
      //      order by "CustomerId" nulls first, "ShipCountry"
      val query = select[Order]
        .where(_.shipCountry isIn ("Poland", "USA"))
        .groupBy(
          _.customerId.cube,
          _.shipCountry.cube,
          _.employeeId
        )
        .aggregate(order => order.sumBy(_.freight) && order.avgBy(_.freight))
        .mapTo(OrderStatsCubePartial)
        .orderBy(_.customerId, _.shipCountry)

      val actualResult = query.toList
//        .show(truncate = false, numRows = 200)
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

      val expectedResult = Fixture.readExpectedResult[OrderStatsCubePartial]("partial_cube")

      assert(actualResult.size == expectedResult.size)

      actualResult should contain theSameElementsAs expectedResult
    }

    "correctly process groupBy with grouping sets" in {
      // SELECT "CustomerId", "ShipCountry", SUM("Freight") as "TotalFreight", AVG("Freight") as "AvgFreight" FROM public."Order"
      //      where "ShipCountry"  in ('Poland', 'USA')
      //      group by grouping sets (
      //        ("CustomerId", "ShipCountry"),
      //        ("CustomerId"),
      //        ("ShipCountry"),
      //        ()
      //      )
      //      order by "CustomerId" nulls first, "ShipCountry"
      val query = select[Order]
        .where(_.shipCountry isIn ("Poland", "USA"))
        .groupByGroupingSets(
          _.customerId,
          _.shipCountry
        )((customerId, shipCountry) =>
          (
            (customerId, shipCountry),
            customerId,
            shipCountry,
            ()
          )
        )
        .aggregate(order => order.sumBy(_.freight) && order.avgBy(_.freight))
        .mapTo(OrderStatsGSets)
        .orderBy(_.customerId, _.shipCountry)

      val actualResult = query.toList
//        .show(truncate = false, numRows = 200)
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

      val expectedResult = Fixture.readExpectedResult[OrderStatsGSets]("grouping_sets")

      assert(actualResult.size == expectedResult.size)

      actualResult should contain theSameElementsAs expectedResult
    }
  }
}
