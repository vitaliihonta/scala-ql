package scalaql.excel

import org.apache.poi.ss.usermodel.{Sheet, Workbook}
import scalaql.Naming
import scalaql.excel.internal.ExcelDataSourceReader
import scalaql.sources.*

import java.io.InputStream

class ExcelReadDsl[A](override val config: ExcelReadConfig)
    extends DataSourceReadDsl[
      A,
      InputStream,
      ExcelDecoder,
      λ[a => ExcelReadConfig],
      ExcelDataSourceReader,
      ExcelReadDsl[A]
    ]
    with DataSourceJavaInputStreamReadDslMixin[
      A,
      ExcelDecoder,
      λ[a => ExcelReadConfig],
      ExcelDataSourceReader,
      ExcelReadDsl[A]
    ]
    with DataSourceFilesReadDslMixin[
      A,
      InputStream,
      ExcelDecoder,
      λ[a => ExcelReadConfig],
      ExcelDataSourceReader,
      ExcelReadDsl[A]
    ] {

  override protected val _reader = ExcelDataSourceReader

  override def withConfig(config: ExcelReadConfig): ExcelReadDsl[A] =
    new ExcelReadDsl[A](config)

  def option(choseWorksheet: Workbook => Sheet): ExcelReadDsl[A] =
    withConfig(config.copy(choseWorksheet = choseWorksheet))

  def option(naming: Naming): ExcelReadDsl[A] =
    withConfig(config.copy(naming = naming))

  def option(evaluateFormulas: Boolean): ExcelReadDsl[A] =
    withConfig(config.copy(evaluateFormulas = evaluateFormulas))

  def option(cellResolutionStrategy: CellResolutionStrategy): ExcelReadDsl[A] =
    withConfig(config.copy(cellResolutionStrategy = cellResolutionStrategy))

  def options(
    naming:                 Naming = config.naming,
    evaluateFormulas:       Boolean = config.evaluateFormulas,
    choseWorksheet:         Workbook => Sheet = config.choseWorksheet,
    cellResolutionStrategy: CellResolutionStrategy = config.cellResolutionStrategy
  ): ExcelReadDsl[A] =
    withConfig(
      config.copy(
        naming = naming,
        evaluateFormulas = evaluateFormulas,
        choseWorksheet = choseWorksheet,
        cellResolutionStrategy = cellResolutionStrategy
      )
    )
}
