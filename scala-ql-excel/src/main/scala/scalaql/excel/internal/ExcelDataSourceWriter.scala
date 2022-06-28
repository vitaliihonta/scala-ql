package scalaql.excel.internal

import org.apache.poi.ss.usermodel.{Cell, CellType, Row}
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import scalaql.SideEffect
import scalaql.excel.ExcelTableApi.ConfigureCell
import scalaql.excel.*
import scalaql.sources.{DataSourceJavaOutputStreamWriteFilesSupport, DataSourceJavaOutputStreamWriter}
import scalaql.sources.columnar.TableApiFunctions
import java.io.OutputStream

object ExcelDataSourceWriter extends ExcelDataSourceWriter

class ExcelDataSourceWriter
    extends DataSourceJavaOutputStreamWriter[ExcelEncoder, ExcelWriteConfig]
    with DataSourceJavaOutputStreamWriteFilesSupport[ExcelEncoder, ExcelWriteConfig] {

  def write[A: ExcelEncoder](
    sink:            => OutputStream
  )(implicit config: ExcelWriteConfig[A]
  ): SideEffect[OutputStream, ?, A] = {
    val workbook  = new XSSFWorkbook()
    val worksheet = workbook.createSheet(config.worksheetName.getOrElse("Default"))
    val table     = new ExcelTableApi(worksheet, ExcelEncoder[A].headers)
    SideEffect[OutputStream, Int, A](
      initialState = 0,
      acquire = () => sink,
      release = { (os, _) =>
        implicit val writeContext: ExcelWriteContext = ExcelWriteContext.initial(
          workbook = workbook,
          headers = ExcelEncoder[A].headers,
          cellStyle = config.styling.cellStyle
        )
        TableApiFunctions.fillGapsIntoTable[Cell, ConfigureCell, ExcelTableRowApi](table)(header =>
          (cell: Cell) => {
            config.styling.cellStyle(header).foreach { styling =>
              val cellStyle = workbook.createCellStyle()
              styling(workbook, cellStyle)
              cell.setCellStyle(cellStyle)
            }
            cell.setBlank()
            cell
          }
        )
        workbook.write(os)
        os.flush()
        os.close()
      },
      use = { (_, rowIdx, value) => writeRow[A](workbook, table, rowIdx, value) }
    )
  }

  private def writeRow[A: ExcelEncoder](
    workbook:        XSSFWorkbook,
    table:           ExcelTableApi,
    rowIdx:          Int,
    value:           A
  )(implicit config: ExcelWriteConfig[A]
  ): Int = {
    val idx = if (rowIdx == 0 && config.writeHeaders) {
      writeHeaders[A](workbook, table)
      rowIdx + 1
    } else rowIdx

    ExcelEncoder[A].write(value, table.appendEmptyRow)(
      ExcelWriteContext.initial(
        workbook = workbook,
        headers = ExcelEncoder[A].headers,
        cellStyle = config.styling.cellStyle
      )
    )
    idx + 1
  }

  private def writeHeaders[A: ExcelEncoder](
    workbook:        XSSFWorkbook,
    table:           ExcelTableApi
  )(implicit config: ExcelWriteConfig[A]
  ): Unit = {
    table.appendEmptyRow
    val headerRow = table.currentRow
    for ((header, idx) <- ExcelEncoder[A].headers.zipWithIndex)
      headerRow.insert(
        idx,
        header,
        (cell: Cell) => {
          config.styling.headerStyle(header).foreach { styling =>
            val cellStyle = workbook.createCellStyle()
            styling(workbook, cellStyle)
            cell.setCellStyle(cellStyle)
          }
          cell.setCellValue(config.naming(header))
          cell
        }
      )
  }
}
