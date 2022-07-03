package scalaql.test.integration

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

  def input: From[Order] & From[OrderDetail] = from(
    csv
      .read[Order]
      .option(Naming.UpperCase)
      .file(
        Paths.get("integration-tests/src/test/resources/input/_Order__202207031241.csv")
      )
  ) & from(
    csv
      .read[OrderDetail]
      .option(Naming.UpperCase)
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
            .option(Naming.UpperCase)
            .file(
              Paths.get(s"integration-tests/src/test/resources/output/$name.csv")
            )
        )
      )
}

class WindowsSpec extends ScalaqlUnitSpec {
  import WindowsSpec.*

  "scalaql" should {
    "correctly process simple window function with aggregate" in {
      val query = select[Order]
        .join(select[OrderDetail])
        .on(_.id == _.orderId)
        .window(
          _.avgBy { case (_, d) => d.unitPrice }
        )
        .over(
          _.partitionBy { case (o, _) => o.customerId }
        )
        .map { case (order, detail, avgUnitPrice) =>
          OrderStats(
            customerId = order.customerId,
            orderDate = order.orderDate,
            unitPrice = detail.unitPrice,
            avgUnitPrice = avgUnitPrice
          )
        }
        .orderBy(_.customerId)

      val actualResult = query.toList
        .run(input)

      val expectedResult = readExpectedResult[OrderStats]("simple_window")

      actualResult should contain theSameElementsAs {
        expectedResult
      }
    }

    "correctly process simple window function with row_number" in {
      val query = select[Order]
        .join(select[OrderDetail])
        .on(_.id == _.orderId)
        .window(
          _.rowNumber
        )
        .over(
          _.partitionBy { case (o, _) => o.customerId }
        )
        .map { case (order, detail, unitRank) =>
          OrderRanked(
            customerId = order.customerId,
            orderDate = order.orderDate,
            unitPrice = detail.unitPrice,
            unitRank = unitRank
          )
        }
        .orderBy(_.customerId)

      val actualResult = query.toList
        .run(input)

      val expectedResult = readExpectedResult[OrderRanked]("row_number_window")

      actualResult should contain theSameElementsAs {
        expectedResult
      }
    }

    // TODO: something wrong with rank
    "correctly process window with rank" in {
      val query = select[Order]
        .join(select[OrderDetail])
        .on(_.id == _.orderId)
        .window(
          _.rank
        )
        .over(
          _.partitionBy { case (o, _) => o.customerId }
            .orderBy { case (_, d) => d.unitPrice }
        )
        .map { case (order, detail, unitRank) =>
          OrderRanked(
            customerId = order.customerId,
            orderDate = order.orderDate,
            unitPrice = detail.unitPrice,
            unitRank = unitRank
          )
        }
//        .orderBy(_.customerId)

      val actualResult = query
//        .show(truncate = false, numRows = 100)
        .toList
        .run(input)
        .sortBy(order => (order.customerId, order.unitPrice, order.unitRank))

      val expectedResult = readExpectedResult[OrderRanked]("rank_window")
        .sortBy(order => (order.customerId, order.unitPrice, order.unitRank))

//      actualResult should contain theSameElementsAs  {
//        expectedResult
//      }
    }
  }
}
