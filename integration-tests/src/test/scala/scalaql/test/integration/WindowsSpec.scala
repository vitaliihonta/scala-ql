package scalaql.test.integration

import org.scalactic.Equality
import scalaql.*
import scalaql.csv.CsvDecoder
import java.nio.file.Paths
import java.time.LocalDate

object WindowsSpec {
  case class Order(
    id:             Long,
    customerId:     String,
    employeeId:     Long,
    orderDate:      LocalDate,
    requiredDate:   LocalDate,
    shippedDate:    Option[LocalDate],
    shipVia:        Long,
    freight:        Double,
    shipName:       String,
    shipAddress:    String,
    shipCity:       String,
    shipRegion:     String,
    shipPostalCode: String,
    shipCountry:    String)

  case class OrderDetail(
    id:        String,
    orderId:   Long,
    productId: Long,
    unitPrice: Double,
    quantity:  Int,
    discount:  Double)

  case class OrderWithDetails(
    order:   Order,
    details: OrderDetail)

  case class OrderStats(
    customerId:   String,
    orderDate:    LocalDate,
    unitPrice:    Double,
    avgUnitPrice: Double)

  case class OrderRanked(
    customerId: String,
    orderDate:  LocalDate,
    unitPrice:  Double,
    unitRank:   Int)

  case class OrderWithLag(
    productId:   Long,
    shipCountry: String,
    orderDate:   LocalDate,
    shippedDate: Option[LocalDate],
    quantity:    Int,
    lag:         Option[Int])

  def input: From[Order] & From[OrderDetail] = from(
    csv
      .read[Order]
      .option(Naming.Capitalize)
      .file(
        Paths.get("integration-tests/src/test/resources/input/_Order__202207031241.csv")
      )
  ) & from(
    csv
      .read[OrderDetail]
      .option(Naming.Capitalize)
      .file(
        Paths.get("integration-tests/src/test/resources/input/OrderDetail_202207031246.csv")
      )
  )

  def readExpectedResult[A: CsvDecoder: Tag](name: String): List[A] =
    select[A].toList
      .run(
        from(
          csv
            .read[A]
            .option(Naming.Capitalize)
            .file(
              Paths.get(s"integration-tests/src/test/resources/output/$name.csv")
            )
        )
      )
}

import WindowsSpec.*

class WindowsSpec extends ScalaqlUnitSpec {

  private implicit val statsEquality: Equality[OrderStats] =
    new Equality[OrderStats] {
      override def areEqual(a: OrderStats, b: Any): Boolean = b match {
        case b: OrderStats =>
          a.customerId == b.customerId && a.orderDate == b.orderDate &&
          a.unitPrice == b.unitPrice && (a.avgUnitPrice === b.avgUnitPrice +- 0.01)
        case _ => false
      }
    }

  "scalaql" should {
    "correctly process simple window function with aggregate" in {
      // SELECT CustomerId,
      //	   OrderDate,
      //       UnitPrice,
      //       AVG(UnitPrice) OVER (PARTITION BY CustomerId) AS AvgUnitPrice
      // FROM [Order]
      // INNER JOIN OrderDetail ON [Order].Id = OrderDetail.OrderId
      val query = select[Order]
        .join(select[OrderDetail])
        .on(_.id == _.orderId)
        .map((OrderWithDetails.apply _).tupled)
        .window(
          _.avgBy(_.details.unitPrice)
        )
        .over(
          _.partitionBy(_.order.customerId)
        )
        .map { case (data, avgUnitPrice) =>
          OrderStats(
            customerId = data.order.customerId,
            orderDate = data.order.orderDate,
            unitPrice = data.details.unitPrice,
            avgUnitPrice = avgUnitPrice
          )
        }

      val actualResult = query.toList
        .run(input)

      val expectedResult = readExpectedResult[OrderStats]("simple_window")

      actualResult should contain theSameElementsAs {
        expectedResult
      }
    }

    "correctly process simple window function with row_number" in {
      // NOTE: SQL produced a bit different result due to ordering within sqlite db.
      val customerIds = List("ALFKI", "ANATR", "ANTO", "BONAP", "EASTC")

      // SELECT CustomerId,
      //       OrderDate,
      //       UnitPrice,
      //       ROW_NUMBER() OVER (PARTITION BY CustomerId ORDER BY UnitPrice) AS UnitRank
      // FROM [Order]
      // INNER JOIN OrderDetail
      // ON [Order].Id = OrderDetail.OrderId
      val query = select[Order]
        .where(_.customerId isInCollection customerIds)
        .join(select[OrderDetail])
        .on(_.id == _.orderId)
        .map((OrderWithDetails.apply _).tupled)
        .window(
          _.rowNumber
        )
        .over(
          _.partitionBy(_.order.customerId)
            .orderBy(_.details.unitPrice)
        )
        .map { case (data, unitRank) =>
          OrderRanked(
            customerId = data.order.customerId,
            orderDate = data.order.orderDate,
            unitPrice = data.details.unitPrice,
            unitRank = unitRank
          )
        }

      val actualResult = query.toList
        .run(input)

      val expectedResult = readExpectedResult[OrderRanked]("row_number_window")
        .filter(_.customerId isInCollection customerIds)

      actualResult should contain theSameElementsAs {
        expectedResult
      }
    }

    "correctly process window with rank" in {
      // SELECT CustomerId,
      //       OrderDate,
      //       UnitPrice,
      //       RANK() OVER (PARTITION BY CustomerId ORDER BY UnitPrice) AS UnitRank
      // FROM [Order]
      // INNER JOIN OrderDetail ON [Order].Id = OrderDetail.OrderId
      val query = select[Order]
        .join(select[OrderDetail])
        .on(_.id == _.orderId)
        .map((OrderWithDetails.apply _).tupled)
        .window(
          _.rank
        )
        .over(
          _.partitionBy(_.order.customerId)
            .orderBy(_.details.unitPrice)
        )
        .map { case (data, unitRank) =>
          OrderRanked(
            customerId = data.order.customerId,
            orderDate = data.order.orderDate,
            unitPrice = data.details.unitPrice,
            unitRank = unitRank
          )
        }

      val actualResult = query.toList
        .run(input)

      val expectedResult = readExpectedResult[OrderRanked]("rank_window")

      actualResult should contain theSameElementsAs {
        expectedResult
      }
    }

    "correctly process window with lag" in {
      // NOTE: SQL produced a bit different result due to ordering within sqlite db.
      val productIds = List(1L, 2L, 3L)

      // SELECT
      //   ProductId,
      //   ShipCountry,
      //   OrderDate,
      //   ShippedDate,
      //   Quantity,
      //   LAG(Quantity) OVER (PARTITION BY ProductId, ShipCountry  ORDER BY OrderDate DESC, ShippedDate DESC) AS Lag
      // FROM [Order]
      // INNER JOIN OrderDetail ON [Order].Id = OrderDetail.OrderId
      // WHERE ProductId IN (1, 2, 3)
      val query = select[Order]
        .join(select[OrderDetail])
        .on(_.id == _.orderId)
        .map((OrderWithDetails.apply _).tupled)
        .where(_.details.productId isInCollection productIds)
        .window(
          _.lag(_.details.quantity)
        )
        .over(
          _.partitionBy(_.details.productId, _.order.shipCountry)
            .orderBy(_.order.orderDate.desc, _.order.shippedDate.desc)
        )
        .map { case (data, lag) =>
          OrderWithLag(
            productId = data.details.productId,
            shipCountry = data.order.shipCountry,
            orderDate = data.order.orderDate,
            shippedDate = data.order.shippedDate,
            quantity = data.details.quantity,
            lag = lag
          )
        }
        .orderBy(o => (o.productId, o.shipCountry, o.orderDate, o.shippedDate))

      val actualResult = query.toList
        .run(input)

      val expectedResult = readExpectedResult[OrderWithLag]("lag_window")
        .filter(_.productId isInCollection productIds)
        .sortBy(o => (o.productId, o.shipCountry, o.orderDate, o.shippedDate))

      actualResult shouldEqual {
        expectedResult
      }
    }
  }
}
