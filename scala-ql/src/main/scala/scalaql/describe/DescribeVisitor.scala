package scalaql.describe

import scala.collection.mutable

trait DescribeVisitor {
  def addCount(field:   String, n:     Int): this.type
  def addNumeric(field: String, value: BigDecimal): this.type

  def getStats: List[RowDescription]
}

object DescribeVisitorImpl {
  def empty: DescribeVisitorImpl = new DescribeVisitorImpl(
    stats = mutable.Map.empty
  )
}

class DescribeVisitorStats(
  var count: Long,
  var sum:   Option[BigDecimal],
  var min:   Option[BigDecimal],
  var max:   Option[BigDecimal]) { self =>

  def addNumeric(value: BigDecimal): Unit = {
    sum = self.sum
      .map(_ + value)
      .orElse(Some(value))

    min = self.min
      .map(_.min(value))
      .orElse(Some(value))

    max = self.max
      .map(_.max(value))
      .orElse(Some(value))
  }
}

object DescribeVisitorStats {
  def empty: DescribeVisitorStats = new DescribeVisitorStats(
    count = 0,
    sum = None,
    min = None,
    max = None
  )
}

class DescribeVisitorImpl(
  stats: mutable.Map[String, DescribeVisitorStats])
    extends DescribeVisitor { self =>

  override def addCount(field: String, n: Int): this.type = {
    if (!stats.contains(field)) {
      stats.update(field, DescribeVisitorStats.empty)
    }
    val stat = stats(field)
    stat.count += 1
    this
  }

  override def addNumeric(field: String, value: BigDecimal): this.type = {
    addCount(field, 1)
    stats(field).addNumeric(value)
    this
  }

  override def getStats: List[RowDescription] =
    stats.map { case (field, fieldStats) =>
      // TODO: calculate
      RowDescription(
        field = field,
        count = fieldStats.count,
        mean = fieldStats.sum.map(_ / fieldStats.count),
        std = None,
        min = fieldStats.min,
        percentile25 = None,
        percentile50 = None,
        percentile75 = None,
        max = fieldStats.max
      )
    }.toList
}

case class RowDescription(
  field:        String,
  count:        Long,
  mean:         Option[BigDecimal],
  std:          Option[BigDecimal],
  min:          Option[BigDecimal],
  percentile25: Option[BigDecimal],
  percentile50: Option[BigDecimal],
  percentile75: Option[BigDecimal],
  max:          Option[BigDecimal])
