package scalaql

import scalaql.sources.columnar.{GenericTableApi, GenericTableRow}

package object visualization {
  type ShowTableRow = GenericTableRow[String]
  type ShowTable    = GenericTableApi[String]
  object ShowTable {
    def empty: ShowTable = GenericTableApi.empty[String]
  }
}
