package scalaql.utils

import spire.math.{Fractional, sqrt}
import spire.syntax.all.*

private[scalaql] object MathUtils {

  case class Std[N](value: N, sum: N, mean: N, count: Int)

  def std[N: Fractional](values: List[N]): Std[N] = {
    val sum            = values.qsum
    val count          = values.size
    val mean           = sum / count
    val squareDistance = values.map(v => (v - mean).fpow(Fractional[N].fromInt(2)))
    val std            = sqrt(squareDistance.qsum / count)
    Std[N](std, sum, mean, count)
  }

  val Percentile25 = 25
  val Percentile75 = 75
  val Percentile90 = 90

  val DefaultPercentiles: List[Int] = List(Percentile25, Percentile75, Percentile90)

  def percentiles[N: Ordering](values: List[N], percents: List[Int] = DefaultPercentiles): Map[Int, N] = {
    val sorted = values.sorted
    percentilesSorted(sorted, percents)
  }

  def percentilesSorted[N: Ordering](sorted: List[N], percents: List[Int] = DefaultPercentiles): Map[Int, N] = {
    val N = sorted.size
    def go(percentile: Int): N = {
      val index = Math.ceil(N * percentile / 100.0).toInt
      sorted(index - 1)
    }
    percents.map(p => p -> go(p)).toMap
  }
}
