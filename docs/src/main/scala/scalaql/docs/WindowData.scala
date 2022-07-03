package scalaql.docs

import java.time.LocalDate

object WindowData {
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
}
