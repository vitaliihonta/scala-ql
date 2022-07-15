package scalaql

import java.util.regex.Pattern

/**
 * Represents different ways of encoding field names in various data formats,
 * such as `CamelCase`, `snake_case`, `kebab-case`, etc.
 * */
sealed trait Naming extends Serializable {

  /**
   * Transforms literal field name into given naming.
   * */
  def apply(s: String): String
}

object Naming {

  private val basePattern: Pattern = Pattern.compile("([A-Z]+)([A-Z][a-z])")
  private val swapPattern: Pattern = Pattern.compile("([a-z\\d])([A-Z])")

  /** Returns `fieldName` as is */
  final case object Literal extends Naming {
    override def apply(s: String): String = s
  }

  /** Transforms `fieldName` into `FieldName` */
  final case object Capitalize extends Naming {
    override def apply(s: String): String = s.capitalize
  }

  /** Transforms `fieldName` into `field_name` */
  final case object SnakeCase extends Naming {
    override def apply(s: String): String = {
      val partial = basePattern.matcher(s).replaceAll("$1_$2")
      swapPattern.matcher(partial).replaceAll("$1_$2").toLowerCase
    }
  }

  /** Transforms `fieldName` into `FIELD_NAME` */
  final case object ScreamingSnakeCase extends Naming {
    override def apply(s: String): String = {
      val partial = basePattern.matcher(s).replaceAll("$1_$2")
      swapPattern.matcher(partial).replaceAll("$1_$2").toUpperCase
    }
  }

  /** Transforms `fieldName` into `field-name` */
  final case object KebabCase extends Naming {
    override def apply(s: String): String = {
      val partial = basePattern.matcher(s).replaceAll("$1-$2")
      swapPattern.matcher(partial).replaceAll("$1-$2").toLowerCase
    }
  }

  /** Transforms `fieldName` into `field Name` */
  final case object WithSpaces extends WithSpaces(identity)

  /** Transforms `fieldName` into `field name` */
  final case object WithSpacesLowerCase extends WithSpaces(_.toLowerCase)

  /** Transforms `fieldName` into `Field Name` */
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
