package scalaql.internal

import scalaql.*
import scala.language.implicitConversions

private[scalaql] object NaturalOrdering extends Ordering[Any] {
  override def compare(x: Any, y: Any): Int = 1
  def apply[A]: Ordering[A]                 = NaturalOrdering.asInstanceOf[Ordering[A]]
}

// DO NOT USE AS SORTED MAP ORDERING, IT MAY PRODUCE STRANGE RESULTS
private[scalaql] class RollupGroupingKeyOrdering(orders: List[Ordering[Any]]) extends Ordering[Query.GroupKeys] {
  override def compare(xs: Query.GroupKeys, ys: Query.GroupKeys): Int =
    if (xs.hashCode == ys.hashCode) 0
    else {
      val sizeX = xs.size
      val sizeY = ys.size

      val keysIter = (xs.keys.keySet intersect ys.keys.keySet).iterator
      var result   = 0
      // Compare (sub)set of keys
      while (keysIter.hasNext && result == 0) {
        val idx   = keysIter.next()
        val x     = xs(idx)
        val y     = ys(idx)
        val order = orders(idx)
        result = order.compare(x, y)
      }

      if (result == 0 && sizeX != sizeY) {
        // if one of keys is subset of another - the shorter one is greater
        sizeY - sizeX
      } else result
    }
}
