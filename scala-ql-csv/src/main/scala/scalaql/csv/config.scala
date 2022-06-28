package scalaql.csv

import scalaql.sources.Naming

sealed abstract class Quoting(private[scalaql] val toTototoshi: com.github.tototoshi.csv.Quoting)
    extends Product
    with Serializable

object Quoting {
  case object QuoteAll        extends Quoting(com.github.tototoshi.csv.QUOTE_ALL)
  case object QuoteMinimal    extends Quoting(com.github.tototoshi.csv.QUOTE_MINIMAL)
  case object QuoteNone       extends Quoting(com.github.tototoshi.csv.QUOTE_NONE)
  case object QuoteNonNumeric extends Quoting(com.github.tototoshi.csv.QUOTE_NONNUMERIC)
}

case class CsvReadConfig(
  delimiter:           Char,
  quoteChar:           Char,
  escapeChar:          Char,
  lineTerminator:      String,
  quoting:             Quoting,
  treatEmptyLineAsNil: Boolean,
  naming:              Naming,
  caseSensitive:       Boolean) { self =>

  protected[scalaql] def toTototoshi: com.github.tototoshi.csv.CSVFormat =
    new com.github.tototoshi.csv.CSVFormat {
      override val delimiter: Char                           = self.delimiter
      override val quoteChar: Char                           = self.quoteChar
      override val escapeChar: Char                          = self.escapeChar
      override val lineTerminator: String                    = self.lineTerminator
      override val quoting: com.github.tototoshi.csv.Quoting = self.quoting.toTototoshi
      override val treatEmptyLineAsNil: Boolean              = self.treatEmptyLineAsNil
    }
}

object CsvReadConfig extends LowPriorityCsvReadConfig {
  val tsv: CsvReadConfig = CsvReadConfig(
    delimiter = '\t',
    quoteChar = '"',
    escapeChar = '\\',
    lineTerminator = "\r\n",
    quoting = Quoting.QuoteNone,
    treatEmptyLineAsNil = false,
    naming = Naming.Literal,
    caseSensitive = false
  )
}

trait LowPriorityCsvReadConfig {
  implicit val default: CsvReadConfig = CsvReadConfig(
    delimiter = ',',
    quoteChar = '"',
    escapeChar = '"',
    lineTerminator = "\r\n",
    quoting = Quoting.QuoteMinimal,
    treatEmptyLineAsNil = false,
    naming = Naming.Literal,
    caseSensitive = false
  )
}

case class CsvWriteConfig(
  delimiter:           Char,
  quoteChar:           Char,
  escapeChar:          Char,
  lineTerminator:      String,
  quoting:             Quoting,
  treatEmptyLineAsNil: Boolean,
  naming:              Naming) { self =>

  protected[scalaql] def toTototoshi: com.github.tototoshi.csv.CSVFormat =
    new com.github.tototoshi.csv.CSVFormat {
      override val delimiter: Char                           = self.delimiter
      override val quoteChar: Char                           = self.quoteChar
      override val escapeChar: Char                          = self.escapeChar
      override val lineTerminator: String                    = self.lineTerminator
      override val quoting: com.github.tototoshi.csv.Quoting = self.quoting.toTototoshi
      override val treatEmptyLineAsNil: Boolean              = self.treatEmptyLineAsNil
    }
}

object CsvWriteConfig extends LowPriorityCsvWriteConfig {
  val tsv: CsvWriteConfig = CsvWriteConfig(
    delimiter = '\t',
    quoteChar = '"',
    escapeChar = '\\',
    lineTerminator = "\r\n",
    quoting = Quoting.QuoteNone,
    treatEmptyLineAsNil = false,
    naming = Naming.Literal
  )
}

trait LowPriorityCsvWriteConfig {
  implicit val default: CsvWriteConfig = CsvWriteConfig(
    delimiter = ',',
    quoteChar = '"',
    escapeChar = '"',
    lineTerminator = "\r\n",
    quoting = Quoting.QuoteMinimal,
    treatEmptyLineAsNil = false,
    naming = Naming.Literal
  )
}
