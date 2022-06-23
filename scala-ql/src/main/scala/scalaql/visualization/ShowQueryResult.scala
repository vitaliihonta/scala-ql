package scalaql.visualization

import org.apache.commons.lang3.StringUtils
import scalaql.SideEffect
import scalaql.internalApi

import scala.collection.mutable

/** Mostly copy-paste from org.apache.spark.sql.Dataset#show
  */
@internalApi
object ShowQueryResult {
  case class ShowState(
    into:        ShowTable,
    rowsWritten: Int,
    writtenAll:  Boolean)

  private val minimumColWidth = 3

  def sideEffect[A: ShowAsTable](
    numRows:  Int,
    truncate: Int
  ): SideEffect.Resourceless[?, A] = {
    implicit val showContext: ShowAsTableContext = ShowAsTableContext.initial(ShowAsTable[A].headers)
    SideEffect
      .resourceless[ShowState, A](
        initialState = ShowState(
          ShowTable.empty,
          rowsWritten = 0,
          writtenAll = true
        ),
        use = (state, value) =>
          if (state.rowsWritten > numRows) {
            state.copy(writtenAll = false)
          } else {
            ShowAsTable[A].write(value, state.into.appendEmptyRow)
            state.copy(rowsWritten = state.rowsWritten + 1)
          }
      )
      .afterAll { (_, state) =>
        val sb = new mutable.StringBuilder
        writeInto(sb, state.into, showContext.headers, truncate)
        if (!state.writtenAll) {
          // For Data that has more than "numRows" records
          val rowsString = if (numRows == 1) "row" else "rows"
          sb.append(s"only showing top $numRows $rowsString\n")
        }
        println(sb.toString)
      }
  }

  private def writeInto(
    sb:       mutable.StringBuilder,
    table:    ShowTable,
    headers:  List[String],
    truncate: Int
  ): Unit = {
    val rows      = table.getRows
    val numCols   = headers.length
    val colWidths = Array.fill(numCols)(minimumColWidth)

    for ((header, i) <- headers.zipWithIndex)
      colWidths(i) = math.max(colWidths(i), stringHalfWidth(header))

    for (row <- rows)
      for (((_, cell), i) <- row.getCells.zipWithIndex)
        colWidths(i) = math.max(colWidths(i), stringHalfWidth(truncated(cell, truncate)))

    val paddedHeaders = headers.zipWithIndex.map { case (header, i) =>
      if (truncate > 0) {
        StringUtils.leftPad(header, colWidths(i) - stringHalfWidth(header) + header.length)
      } else {
        StringUtils.rightPad(header, colWidths(i) - stringHalfWidth(header) + header.length)
      }
    }

    val paddedRows = rows.map { row =>
      row.getCells.zipWithIndex.map { case ((_, cell), i) =>
        val cellStr = truncated(cell, truncate)
        if (truncate > 0) {
          StringUtils.leftPad(cellStr, colWidths(i) - stringHalfWidth(cellStr) + cellStr.length)
        } else {
          StringUtils.rightPad(cellStr, colWidths(i) - stringHalfWidth(cellStr) + cellStr.length)
        }
      }
    }

    // Create SeparateLine
    val sep: String = colWidths.map("-" * _).addString(sb, "+", "+", "+\n").toString()

    // column names
    paddedHeaders.addString(sb, "|", "|", "|\n")
    sb.append(sep)

    // data
    paddedRows.foreach(_.addString(sb, "|", "|", "|\n"))
    sb.append(sep)
  }

  private val fullWidthRegex = ("""[""" +
    // scalastyle:off nonascii
    "\u1100-\u115F" +
    "\u2E80-\uA4CF" +
    "\uAC00-\uD7A3" +
    "\uF900-\uFAFF" +
    "\uFE10-\uFE19" +
    "\uFE30-\uFE6F" +
    "\uFF00-\uFF60" +
    "\uFFE0-\uFFE6" +
    // scalastyle:on nonascii
    """]""").r

  /** Return the number of half widths in a given string. Note that a full width character occupies two half widths.
    *
    * For a string consisting of 1 million characters, the execution of this method requires about 50ms.
    */
  private def stringHalfWidth(str: String): Int =
    if (str == null) 0 else str.length + fullWidthRegex.findAllIn(str).size

  private def truncated(str: String, truncate: Int): String =
    if (truncate > 0 && str.length > truncate) {
      // do not show ellipses for strings shorter than 4 characters.
      if (truncate < 4) str.substring(0, truncate)
      else str.substring(0, truncate - 3) + "..."
    } else {
      str
    }
}
