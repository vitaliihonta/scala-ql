package scalaql.test.integration

import scalaql.*
import scalaql.csv.CsvDecoder
import java.nio.file.Paths
import java.time.LocalDate

object Fixture {

  def readExpectedResult[A: CsvDecoder: Tag](name: String): List[A] =
    select[A].toList
      .run(
        from(
          csv
            .read[A]
            .options(
              naming = Naming.Capitalize,
              emptyStringInOptions = false
            )
            .file(
              Paths.get(s"integration-tests/src/test/resources/output/$name.csv")
            )
        )
      )
}

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
