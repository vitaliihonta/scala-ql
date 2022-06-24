package scalaql.docs

import java.time.LocalDate

object ExcelData {
  case class OrderInfo(
    orderDate: LocalDate,
    region:    String,
    rep:       String,
    item:      String,
    units:     Int,
    unitCost:  BigDecimal,
    total:     BigDecimal)
}
