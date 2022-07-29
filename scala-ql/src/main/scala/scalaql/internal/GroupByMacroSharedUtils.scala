package scalaql.internal

import scalaql.Query

private[scalaql] object GroupByMacroSharedUtils {

  sealed trait GroupingKind
  case object KindSimple extends GroupingKind
  case object KindRollup extends GroupingKind
  case object KindCube   extends GroupingKind

  /** @tparam Tree the concrete tree implementation */
  case class GroupingMeta[Tree](
    groupFuncBody:    Tree,
    kind:             GroupingKind,
    ordering:         Tree,
    groupFillments:   Tree,
    defaultFillments: Option[Tree])

  def buildGroupingSets[Tree](
    metas:          List[GroupingMeta[Tree]]
  )(error:          String => Nothing,
    toGroupFills:   (GroupingMeta[Tree], Int) => Tree,
    toDefaultFills: (Tree, Int) => Tree,
    buildTree:      (List[Query.GroupingSetIndices], List[Tree], List[Tree], List[Tree]) => Tree
  ): Tree = {
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
        List(Query.GroupingSetIndices(metasWithIndex.map { case (_, idx) => idx }))
      } else {
        nonSimple.head match {
          case KindSimple =>
            error(FatalExceptions.libraryErrorMessage(s"Non-simple grouping kinds contains KindSimple metas=$metas"))
          case KindRollup if allNonSimple =>
            metasWithIndex.tails.map(tail => Query.GroupingSetIndices(tail.map { case (_, idx) => idx })).toList
          case KindCube | KindRollup =>
            val (partialKeys, subtotalKeys) = metasWithIndex.partition { case (m, _) => m.kind == KindSimple }

            val subtotals = (1 until subtotalKeys.size).flatMap { n =>
              subtotalKeys
                .map { case (_, idx) => idx }
                .combinations(n)
                .filterNot(_.isEmpty)
                .map(sub => Query.GroupingSetIndices((sub.toList ++ partialKeys.map { case (_, idx) => idx }).distinct))
            }.toList

            val partial = Query.GroupingSetIndices(partialKeys.map { case (_, idx) => idx })
            val all     = Query.GroupingSetIndices(metasWithIndex.map { case (_, idx) => idx })

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
