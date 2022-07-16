package scalaql.internal

private[scalaql] class RollupGroupingKeyOrdering(orders: List[Ordering[Any]]) extends Ordering[List[Any]] {
  override def compare(xs: List[Any], ys: List[Any]): Int = {
    val sizeX = xs.size
    val sizeY = ys.size

    val sizeDiff = math.abs(sizeX - sizeY)
    // In case of subsets - drop first N elements
    var (idxX, idxY) =
      if (sizeX > sizeY) (sizeDiff, 0)
      else if (sizeX < sizeY) (0, sizeDiff)
      else (0, 0)

    // In case of topper-level aggregations, shift the order to the right
    var idxOrder = sizeDiff + (orders.length - math.max(sizeX, sizeY))

    var result = 0
    // Compare (sub)set of keys
    while (idxX < sizeX && idxY < sizeY && result == 0) {
      val x     = xs(idxX)
      val y     = ys(idxY)
      val order = orders(idxOrder)
      result = order.compare(x, y)

      // increment all counters
      idxX += 1
      idxY += 1
      idxOrder += 1
    }

    if (result == 0 && sizeX != sizeY) {
      // if one of keys is subset of another - the shorter one is greater
      sizeY - sizeX
    } else result
  }
}
