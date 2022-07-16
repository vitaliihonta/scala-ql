package scalaql.internal

object FatalExceptions {
  def libraryError(details: String): Throwable =
    new RuntimeException(s"Library error!\n$details.\nPlease fill up a ticket on github")

  def emptyGroupByResult(details: String): Throwable =
    libraryError(s"GroupBy should not produce an empty group. $details")
}
