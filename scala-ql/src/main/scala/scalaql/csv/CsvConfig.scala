package scalaql.csv

import com.github.tototoshi.{csv => tototoshi}

sealed abstract class Quoting(private[scalaql] val toTototoshi: tototoshi.Quoting) extends Product with Serializable

object Quoting {
  case object QuoteAll        extends Quoting(tototoshi.QUOTE_ALL)
  case object QuoteMinimal    extends Quoting(tototoshi.QUOTE_MINIMAL)
  case object QuoteNone       extends Quoting(tototoshi.QUOTE_NONE)
  case object QuoteNonNumeric extends Quoting(tototoshi.QUOTE_NONNUMERIC)
}

case class CsvConfig(
  delimiter:           Char,
  quoteChar:           Char,
  escapeChar:          Char,
  lineTerminator:      String,
  quoting:             Quoting,
  treatEmptyLineAsNil: Boolean) { self =>

  private[scalaql] def toTototoshi: tototoshi.CSVFormat =
    new tototoshi.CSVFormat {
      override val delimiter: Char              = self.delimiter
      override val quoteChar: Char              = self.quoteChar
      override val escapeChar: Char             = self.escapeChar
      override val lineTerminator: String       = self.lineTerminator
      override val quoting: tototoshi.Quoting   = self.quoting.toTototoshi
      override val treatEmptyLineAsNil: Boolean = self.treatEmptyLineAsNil
    }
}

object CsvConfig {

  val default: CsvConfig = CsvConfig(
    delimiter = ',',
    quoteChar = '"',
    escapeChar = '"',
    lineTerminator = "\r\n",
    quoting = Quoting.QuoteMinimal,
    treatEmptyLineAsNil = false
  )

  val tsv: CsvConfig = CsvConfig(
    delimiter = '\t',
    quoteChar = '"',
    escapeChar = '\\',
    lineTerminator = "\r\n",
    quoting = Quoting.QuoteNone,
    treatEmptyLineAsNil = false
  )
}
