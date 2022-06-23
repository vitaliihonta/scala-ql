package scalaql.describe

import scalaql.utils.MathUtils
import scala.collection.mutable
import spire.math.Fractional

trait DescribeVisitor {
  def addNonNumeric(field: String, value: Any): this.type

  def addOrdered[A: Ordering](field:                 String, value: A): this.type
  def addNumeric[N: Fractional: ToBigDecimal](field: String, value: N): this.type

  def getStats: List[RowDescription]
}

object DescribeVisitorImpl {
  def empty(config: DescribeConfig): DescribeVisitorImpl = new DescribeVisitorImpl(
    config = config,
    stats = mutable.Map.empty
  )
}

sealed trait Stats {
  def build(field: String, config: DescribeConfig): RowDescription
}

class NumericStats[N](
  private[scalaql] val values:       mutable.ListBuffer[N],
  private[scalaql] val fractional:   Fractional[N],
  private[scalaql] val toBigDecimal: ToBigDecimal[N])
    extends Stats {

  override def build(field: String, config: DescribeConfig): RowDescription = {
    val (stdOpt, minOpt, maxOpt, percentiles) =
      if (values.isEmpty) (None, None, None, Map.empty[Int, N])
      else {
        val ordering     = fractional.toOrdering
        val valuesSorted = values.toList.sorted(ordering)
        (
          Some(MathUtils.std(valuesSorted)(fractional)),
          Some(valuesSorted.head),
          Some(valuesSorted.last),
          MathUtils.percentilesSorted[N](valuesSorted)(ordering)
        )
      }

    def showValue(value: N): String =
      toBigDecimal(value).round(config.precision).toString

    RowDescription(
      field = field,
      count = values.size,
      mean = stdOpt.map(s => showValue(s.mean)),
      std = stdOpt.map(s => showValue(s.value)),
      min = minOpt.map(showValue),
      percentile25 = percentiles.get(MathUtils.Percentile25).map(showValue),
      percentile75 = percentiles.get(MathUtils.Percentile75).map(showValue),
      percentile90 = percentiles.get(MathUtils.Percentile90).map(showValue),
      max = maxOpt.map(showValue),
      unique = Set.empty
    )
  }
}

class OrderedStats[A](
  private[scalaql] val values:   mutable.ListBuffer[A],
  private[scalaql] val ordering: Ordering[A])
    extends Stats {

  override def build(field: String, config: DescribeConfig): RowDescription = {
    val (minOpt, maxOpt) =
      if (values.isEmpty) (None, None)
      else {
        (
          Some(values.min(ordering)),
          Some(values.max(ordering))
        )
      }

    RowDescription(
      field = field,
      count = values.size,
      mean = None,
      std = None,
      min = minOpt.map(_.toString),
      percentile25 = None,
      percentile75 = None,
      percentile90 = None,
      max = maxOpt.map(_.toString),
      unique = Set.empty
    )
  }
}

class DefaultStats[A](
  private[scalaql] val values: mutable.ListBuffer[A])
    extends Stats {

  override def build(field: String, config: DescribeConfig): RowDescription =
    RowDescription(
      field,
      count = values.size,
      mean = None,
      std = None,
      min = None,
      percentile25 = None,
      percentile75 = None,
      percentile90 = None,
      max = None,
      unique = if (!config.unique) Set.empty else values.toSet[Any].map(_.toString)
    )
}

class DescribeVisitorStats(
  private[scalaql] var count:        Int,
  private[scalaql] var numericStats: Option[NumericStats[Any]],
  private[scalaql] var orderedStats: Option[OrderedStats[Any]],
  private[scalaql] var defaultStats: Option[DefaultStats[Any]])

object DescribeVisitorStats {
  def empty: DescribeVisitorStats = new DescribeVisitorStats(
    count = 0,
    numericStats = None,
    orderedStats = None,
    defaultStats = None
  )
}

class DescribeVisitorImpl(
  config: DescribeConfig,
  stats:  mutable.Map[String, DescribeVisitorStats])
    extends DescribeVisitor { self =>

  private def addCount(field: String): this.type = {
    if (!stats.contains(field)) {
      stats.update(field, DescribeVisitorStats.empty)
    }
    val stat = stats(field)
    stat.count += 1
    this
  }

  override def addNonNumeric(field: String, value: Any): DescribeVisitorImpl.this.type = {
    addCount(field)
    val stat = stats(field)
    stat.defaultStats = stat.defaultStats
      .map { ds =>
        ds.values += value
        ds
      }
      .orElse(Some(new DefaultStats[Any](mutable.ListBuffer(value))))
    this
  }

  override def addNumeric[N: Fractional: ToBigDecimal](field: String, value: N): this.type = {
    addCount(field)
    val stat = stats(field)
    stat.numericStats = stat.numericStats
      .map { ns =>
        ns.values += value
        ns
      }
      .orElse(
        Some(
          new NumericStats[N](mutable.ListBuffer(value), Fractional[N], ToBigDecimal[N])
        ).asInstanceOf[Option[NumericStats[Any]]]
      )

    this
  }

  override def addOrdered[A: Ordering](field: String, value: A): this.type = {
    addCount(field)
    val stat = stats(field)
    stat.orderedStats = stat.orderedStats
      .map { os =>
        os.values += value
        os
      }
      .orElse(
        Some(
          new OrderedStats[A](mutable.ListBuffer(value), Ordering[A])
        ).asInstanceOf[Option[OrderedStats[Any]]]
      )

    this
  }

  override def getStats: List[RowDescription] =
    stats.map { case (field, fieldStats) =>
      fieldStats.numericStats
        .map(_.build(field, config))
        .orElse(fieldStats.orderedStats.map(_.build(field, config)))
        .orElse(fieldStats.defaultStats.map(_.build(field, config)))
        .getOrElse(
          RowDescription(
            field = field,
            count = 0,
            mean = None,
            std = None,
            min = None,
            percentile25 = None,
            percentile75 = None,
            percentile90 = None,
            max = None,
            unique = Set.empty
          )
        )
    }.toList
}

case class RowDescription(
  field:        String,
  count:        Int,
  mean:         Option[String],
  std:          Option[String],
  min:          Option[String],
  percentile25: Option[String],
  percentile75: Option[String],
  percentile90: Option[String],
  max:          Option[String],
  unique:       Set[String])
