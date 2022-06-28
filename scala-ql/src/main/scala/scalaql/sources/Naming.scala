package scalaql.sources

import java.util.regex.Pattern

sealed trait Naming {
  def apply(s: String): String
}

object Naming {

  private val basePattern: Pattern = Pattern.compile("([A-Z]+)([A-Z][a-z])")
  private val swapPattern: Pattern = Pattern.compile("([a-z\\d])([A-Z])")

  final case object Literal extends Naming {
    override def apply(s: String): String = s
  }

  final case object UpperCase extends Naming {
    override def apply(s: String): String = s.capitalize
  }

  final case object SnakeCase extends Naming {
    override def apply(s: String): String = {
      val partial = basePattern.matcher(s).replaceAll("$1_$2")
      swapPattern.matcher(partial).replaceAll("$1_$2").toLowerCase
    }
  }

  final case object ScreamingSnakeCase extends Naming {
    override def apply(s: String): String = {
      val partial = basePattern.matcher(s).replaceAll("$1_$2")
      swapPattern.matcher(partial).replaceAll("$1_$2").toUpperCase
    }
  }

  final case object KebabCase extends Naming {
    override def apply(s: String): String = {
      val partial = basePattern.matcher(s).replaceAll("$1-$2")
      swapPattern.matcher(partial).replaceAll("$1-$2").toLowerCase
    }
  }

  final case object WithSpacesLowerCase  extends WithSpaces(_.toLowerCase)
  final case object WithSpacesCapitalize extends WithSpaces(_.capitalize)

  class WithSpaces(withCase: String => String) extends Naming {
    override def apply(s: String): String = {
      val partial = basePattern.matcher(s).replaceAll("$1 $2")
      withCase {
        swapPattern.matcher(partial).replaceAll("$1 $2")
      }
    }
  }
}
