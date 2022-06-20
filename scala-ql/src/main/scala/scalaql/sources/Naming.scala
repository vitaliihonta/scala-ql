package scalaql.sources

import java.util.regex.Pattern

object Naming {

  private val basePattern: Pattern = Pattern.compile("([A-Z]+)([A-Z][a-z])")
  private val swapPattern: Pattern = Pattern.compile("([a-z\\d])([A-Z])")

  val Literal: Naming = identity[String]

  val SnakeCase: Naming = s => {
    val partial = basePattern.matcher(s).replaceAll("$1_$2")
    swapPattern.matcher(partial).replaceAll("$1_$2").toLowerCase
  }

  val ScreamingSnakeCase: Naming = s => {
    val partial = basePattern.matcher(s).replaceAll("$1_$2")
    swapPattern.matcher(partial).replaceAll("$1_$2").toUpperCase
  }

  val KebabCase: Naming = s => {
    val partial = basePattern.matcher(s).replaceAll("$1-$2")
    swapPattern.matcher(partial).replaceAll("$1-$2").toLowerCase
  }

  val WithSpacesLowerCase: Naming  = withSpaces(_.toLowerCase)
  val WithSpacesCapitalize: Naming = withSpaces(_.capitalize)

  def withSpaces(withCase: String => String): Naming = s => {
    val partial = basePattern.matcher(s).replaceAll("$1 $2")
    withCase {
      swapPattern.matcher(partial).replaceAll("$1 $2")
    }
  }
}
