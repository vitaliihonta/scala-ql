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
    buffer:      mutable.ListBuffer[Iterable[String]],
    rowsWritten: Int,
    writtenAll:  Boolean,
    numCols:     Option[Int],
    colWidths:   Option[Array[Int]],
    sep:         Option[String])

  private val minimumColWidth = 3

  def sideEffect[A: ShowAsTable](
    numRows:  Int,
    truncate: Int
  ): SideEffect.Resourceless[ShowState, A] =
    SideEffect
      .resourceless[ShowState, A](
        initialState = ShowState(
          mutable.ListBuffer.empty[Iterable[String]],
          rowsWritten = 0,
          writtenAll = true,
          numCols = None,
          colWidths = None,
          sep = None
        ),
        use = (state, value) =>
          if (state.rowsWritten > numRows) {
            state.copy(writtenAll = false)
          } else {
            val row       = ShowAsTable[A].toRow(value)
            val numCols   = state.numCols.getOrElse(row.size)
            val colWidths = state.colWidths.getOrElse(Array.fill(numCols)(minimumColWidth))
            for (((_, cell), i) <- row.zipWithIndex)
              colWidths(i) = math.max(colWidths(i), stringHalfWidth(truncated(cell, truncate)))

            if (state.rowsWritten == 0) {
              state.buffer += row.keys
              for ((header, i) <- row.keys.zipWithIndex)
                colWidths(i) = math.max(colWidths(i), stringHalfWidth(header))
            }
            state.buffer += row.values
            state.copy(
              rowsWritten = state.rowsWritten + 1,
              numCols = Some(numCols),
              colWidths = Some(colWidths)
            )
          }
      )
      .afterAll { (_, state) =>
        val sb = new mutable.StringBuilder
        writeInto(sb, state.buffer, state.colWidths.get, truncate)
        if (!state.writtenAll) {
          // For Data that has more than "numRows" records
          val rowsString = if (numRows == 1) "row" else "rows"
          sb.append(s"only showing top $numRows $rowsString\n")
        }
        println(sb.toString)
      }

  private def writeInto(
    sb:        mutable.StringBuilder,
    rows:      mutable.ListBuffer[Iterable[String]],
    colWidths: Array[Int],
    truncate:  Int
  ): Unit = {
    val paddedRows = rows.toList.map { row =>
      row.zipWithIndex.map { case (cell, i) =>
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
    paddedRows.head.addString(sb, "|", "|", "|\n")
    sb.append(sep)

    // data
    paddedRows.tail.foreach(_.addString(sb, "|", "|", "|\n"))
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
