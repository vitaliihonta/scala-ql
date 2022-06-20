package scalaql.excel

import scala.deriving.Mirror
import magnolia1.*
import org.apache.poi.ss.usermodel.Row

trait ExcelRowDecoderAutoDerivation extends ProductDerivation[ExcelDecoder] {

  def join[T](ctx: CaseClass[ExcelDecoder, T]): ExcelDecoder[T] = new ExcelDecoder[T] {

    override def read(row: Row)(implicit readerContext: ExcelReadContext): ExcelDecoder.Result[T] =
      readerContext.cellResolutionStrategy match {
        case _: CellResolutionStrategy.NameBased =>
          // We could only accumulate errors when refering by name,
          // because index-based resolution depends on how much fields being read
          decodeAccumulating(row)
        case _ => decodeFailFast(row)
      }

    private def decodeFailFast(row: Row)(implicit readerContext: ExcelReadContext): ExcelDecoder.Result[T] =
      ctx.params
        .foldLeft[Either[ExcelDecoderException, (Seq[Any], Int)]](Right(Seq.empty[Any] -> 0)) { (acc, param) =>
          acc.flatMap { case (fields, readCells) =>
            param.typeclass
              .read(row)(
                readerContext.copy(
                  path = param.label :: readerContext.path,
                  currentOffset = readerContext.currentOffset + readCells
                )
              )
              .map { case ReadResult(result, read) =>
                (fields :+ result, readCells + read)
              }
          }
        }
        .map { case (fields, readCells) => ReadResult(ctx.rawConstruct(fields), readCells) }

    private def decodeAccumulating(row: Row)(implicit readerContext: ExcelReadContext): ExcelDecoder.Result[T] = {
      val readAllResult: Seq[Either[ExcelDecoderException, ReadResult[Any]]] = ctx.params.map { param =>
        param.typeclass
          .read(row)(
            readerContext.copy(
              path = param.label :: readerContext.path
            )
          )
      }

      if (readAllResult.forall(_.isRight)) {
        val results = readAllResult.collect { case Right(v) => v }
        Right(ReadResult(ctx.rawConstruct(results.map(_.value)), results.map(_.readCells).sum))
      } else {
        val errors = readAllResult.collect { case Left(e) => e }.toList
        Left(
          new ExcelDecoderAccumulatingException(s"${ctx.typeInfo.short} (at ${readerContext.pathStr})", errors)
        )
      }
    }
  }

  inline given autoDerive[T](using Mirror.Of[T]): ExcelDecoder[T] = derived[T]
}
