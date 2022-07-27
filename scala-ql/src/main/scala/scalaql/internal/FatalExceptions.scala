package scalaql.internal

object FatalExceptions {
  def libraryErrorMessage(details: String): String =
    s"Library error!\n$details.\nPlease fill up a ticket on github"

  def libraryError(details: String): Throwable =
    new RuntimeException(libraryErrorMessage(details))

  def macroDslErrorMessage(details: String): String =
    s"Invalid usage of scalaql DSL: $details"

  def invalidLibraryUsageMessage(details: String): String =
    s"Invalid library usage!\n$details\nPlease refer to documentation or fill up a ticket on github if this is a bug"

  def invalidLibraryUsage(details: String): Throwable =
    new RuntimeException(invalidLibraryUsageMessage(details))

  def emptyGroupByResult(details: String): Throwable =
    libraryError(s"GroupBy should not produce an empty group. $details")
}
