package scalaql.internal

private[scalaql] class ChainedOrdering[A](orders: List[(A => Any, Ordering[Any])]) extends Ordering[A] {
  override def compare(x: A, y: A): Int = {
    val iter   = orders.iterator
    var result = 0
    while (iter.hasNext && result == 0) {
      val (f, ordering) = iter.next()
      result = ordering.compare(f(x), f(y))
    }
    result
  }
}
