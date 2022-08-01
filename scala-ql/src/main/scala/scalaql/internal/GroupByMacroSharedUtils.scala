package scalaql.internal

import scalaql.Query

object GroupByMacroSharedUtils {

  sealed trait GroupingKind
  case object KindSimple extends GroupingKind
  case object KindRollup extends GroupingKind
  case object KindCube   extends GroupingKind

  /** @tparam Tree the concrete tree implementation */
  case class GroupingMeta[Expr[_], A, B](
    groupFuncBody:    Expr[A => B],
    kind:             GroupingKind,
    ordering:         Expr[Ordering[Any]],
    groupFillments:   Expr[Any => Any],
    defaultFillments: Option[Expr[Any]]) {
    def widen: GroupingMeta[Expr, Any, Any] = this.asInstanceOf[GroupingMeta[Expr, Any, Any]]
  }

  def buildGroupingSets[Expr[_]](
    metas:          List[GroupingMeta[Expr, Any, Any]]
  )(error:          String => Nothing,
    toGroupFills:   (GroupingMeta[Expr, Any, Any], Int) => Expr[(Int, Any => Any)],
    toDefaultFills: (Expr[Any], Int) => Expr[(Int, Any)],
    buildTree: (
      List[Query.GroupingSetIndices],
      List[Expr[Ordering[Any]]],
      List[Expr[(Int, Any => Any)]],
      List[Expr[(Int, Any)]]
    ) => Expr[Query.GroupingSetsDescription]
  ): Expr[Query.GroupingSetsDescription] = {
    val kinds     = metas.map(_.kind).toSet
    val nonSimple = kinds - KindSimple
    if (nonSimple.size > 1) {
      error(
        s"It is not allowed to mix ${nonSimple.mkString(" and ")} in a single groupBy. Try to express this logic with grouping sets"
      )
    }
    val metasWithIndex = metas.zipWithIndex
    //    println(s"metasWithIndex=$metasWithIndex")
    val sets = {
      val isSimple     = nonSimple.isEmpty
      def allNonSimple = metas.forall(_.kind != KindSimple)
      if (isSimple) {
        List(metasWithIndex.map { case (_, idx) => idx })
      } else {
        nonSimple.head match {
          case KindSimple =>
            error(FatalExceptions.libraryErrorMessage(s"Non-simple grouping kinds contains KindSimple metas=$metas"))
          case KindRollup if allNonSimple =>
            metasWithIndex.tails.map(tail => tail.map { case (_, idx) => idx }).toList
          case KindCube | KindRollup =>
            val (partialKeys, subtotalKeys) = metasWithIndex.partition { case (m, _) => m.kind == KindSimple }

            val subtotals = (1 until subtotalKeys.size).flatMap { n =>
              subtotalKeys
                .map { case (_, idx) => idx }
                .combinations(n)
                .filterNot(_.isEmpty)
                .map(sub => (sub.toList ++ partialKeys.map { case (_, idx) => idx }).distinct)
            }.toList

            val partial = partialKeys.map { case (_, idx) => idx }
            val all     = metasWithIndex.map { case (_, idx) => idx }

            (all :: partial :: subtotals).distinct.reverse
        }
      }
    }

    val orderings    = metas.map(_.ordering)
    val groupFills   = metasWithIndex.map { case (m, idx) => toGroupFills(m, idx) }
    val defaultFills = metasWithIndex.flatMap { case (m, idx) => m.defaultFillments.map(toDefaultFills(_, idx)) }

    buildTree(
      sets,
      orderings,
      groupFills,
      defaultFills
    )
  }
}
