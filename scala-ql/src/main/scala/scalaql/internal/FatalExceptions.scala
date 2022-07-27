package scalaql.internal

object FatalExceptions {
  def libraryErrorMessage(details: String): String =
    s"Library error!\n$details.\nPlease fill up a ticket on github"

  def libraryError(details: String): Throwable =
    new RuntimeException(libraryErrorMessage(details))

  def emptyGroupByResult(details: String): Throwable =
    libraryError(s"GroupBy should not produce an empty group. $details")
}
