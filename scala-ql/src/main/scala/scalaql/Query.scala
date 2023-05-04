package scalaql

import izumi.reflect.macrortti.LightTypeTag
import scalaql.internal.{NaturalOrdering, PartialFunctionAndThenCompat}
import scalaql.utils.TupleFlatten
import scala.util.hashing.MurmurHash3

/** 
  * Query is a description of computations you want to perform on your data.
  * It doesn't evaluate until you explicitly run it.
  *
  * @tparam In query input, e.g. type of the data source
  * @tparam Out query computation result type
  */
sealed abstract class Query[-In: Tag, +Out: Tag] extends Serializable {

  /**
   * Provides information about the query "plan".
   * 
   * @return the query plain
   * */
  def explain: QueryExplain

  override final def toString: String = explain.toString

  /**
   * Transforms the query output by applying an arbitrary function
   * to previous query step.
   *
   * Example: 
   * {{{
   *   select[Person].map(_.age)
   * }}}
   *
   * @tparam B new query output type
   * @param f transformation function
   * @return transformed query
   * */
  def map[B: Tag](f: Out => B): Query[In, B] =
    new Query.MapQuery[In, Out, B](this, f, Tag[B].tag)

  /**
   * Transforms the query output by applying an arbitrary partial function
   * to previous query step.
   *
   * Used to apply transformation and filtering in one step.
   *
   * Example: 
   * {{{
   *   select[Person].collect {
   *     case person if person.age >= 18 =>
   *       person.name
   *   }
   * }}}
   *
   * @tparam B new query output type
   * @param pf transformation function
   * @return transformed query
   * */
  def collect[B: Tag](pf: PartialFunction[Out, B]): Query[In, B] =
    new Query.CollectQuery[In, Out, B](this, pf, Tag[B].tag)

  /**
   * Filters records by specified predicate function.
   *
   * Example: 
   * {{{
   *   select[Person].where(_.age >= 18)
   * }}}
   *
   * @param p predicate
   * @return query producing only elements for which the given predicate holds
   * */
  def where(p: Predicate[Out]): Query[In, Out] =
    new Query.WhereQuery[In, Out](this, p)

  /** @see [[where]] */
  def withFilter(p: Predicate[Out]): Query[In, Out] = where(p)

  /**
   * An equivalent `where(!p)`.
   *
   * @see [[where]]
   * */
  def whereNot(p: Predicate[Out]): Query[In, Out] =
    new Query.WhereQuery[In, Out](this, !p(_), nameHint = Some("WHERE_NOT"))

  /**
   * Transforms the query output by applying an arbitrary function
   * to previous query step.
   *
   * Unlike `map`, expects a transformation which produces multiple results.
   *
   * Example: 
   * {{{
   *   select[Employee].mapConcat(_.skills)
   * }}}
   *
   * @tparam B new query output type
   * @param f transformation function
   * @return transformed query
   * */
  def mapConcat[B: Tag](f: Out => Iterable[B]): Query[In, B] =
    new Query.FlatMapQuery[In, Out, B](
      this,
      out => new Query.Const[B](f(out)),
      Tag[In].tag,
      Tag[B].tag,
      nameHint = Some("MAP_CONCAT")
    )

  /**
   * Transforms the query output by applying an arbitrary function
   * which produces a new Query based on the previous step.
   *
   * Example: 
   * {{{
   *   select[Employee]
   *     .flatMap { employee =>
   *       select[Skill].where(_.name isInCollection employee.skills)
   *     }
   * }}}
   *
   * @tparam In2 inferred common input type for this query and the transformed result query
   * @tparam B new query output type
   * @param f transformation function
   * @return transformed query
   * */
  def flatMap[In2 <: In: Tag, B: Tag](f: Out => Query[In2, B]): Query[In2, B] =
    new Query.FlatMapQuery[In2, Out, B](this, f, Tag[In2].tag, Tag[B].tag)

  /**
   * Filters records by a predicate which depends on the result of other query.
   *
   * Example: 
   * {{{
   *   select[Person]
   *     .whereSubQuery(person =>
   *       select[Employee].exists(_.personId == person.id)
   *     )
   * }}}
   *
   * @tparam In2 inferred common input type for this query and the transformed result query
   * @param p predicate
   * @return query producing only elements for which the given predicate holds
   * */
  def whereSubQuery[In2 <: In: Tag](p: Out => QueryResult[In2, Boolean]): Query[In2, Out] =
    new Query.WhereSubQuery[In2, Out](this, p)

  /**
   * Accumulates all of the query values into a state,
   * producing records based on that state.
   * 
   * @note should be used only for complicated computations if no equivalent exist.
   *       Accumulating query emits value only after processing the whole input!
   * 
   * Example: 
   * {{{
   *   val uniqueSkills = select[Person]
   *     .accumulate(initialState = Set.empty[Skill])(
   *       (skillSet, person) =>
   *         skillSet ++ person.skills
   *     )(_.toList)
   * }}}
   *
   * @tparam S accumulation state type
   * @tparam B accumulation result type
   * @param initialState the initial state for accumulation
   * @param modifyState updates state by applying query output value
   * @param getResults extract records from the state
   * @return transformed query
   * */
  def accumulate[S: Tag, B: Tag](
    initialState: S
  )(modifyState:  (S, Out) => S
  )(getResults:   S => Iterable[B]
  ): Query[In, B] =
    new Query.Accumulate[In, Out, S, B](
      this,
      initialState,
      modifyState,
      getResults,
      Tag[S].tag
    )

  /**
   * Transforms the query output by applying an arbitrary stateful function
   * to previous query step.
   *
   * Unlike `map`, each output value depends on previously computed state.
   *
   * @tparam S accumulation state type
   * @tparam B accumulation result type
   * @param initialState the initial state for accumulation
   * @param process computes output value based on the input value and state
   * @return transformed query
   * */
  def statefulMap[S: Tag, B: Tag](
    initialState: S
  )(process:      (S, Out) => (S, B)
  ): Query[In, B] =
    new Query.StatefulMapConcat[In, Out, S, B](
      this,
      initialState,
      process = { (state, out) =>
        val (newState, next) = process(state, out)
        newState -> List(next)
      },
      Tag[S].tag,
      nameHint = Some(s"STATEFUL_MAP($initialState)")
    )

  /**
   * Transforms the query output by applying an arbitrary stateful function
   * to previous query step.
   *
   * Unlike `statefulMap`, expects a stateful transformation which produces multiple results.
   *
   * @tparam S accumulation state type
   * @tparam B accumulation result type
   * @param initialState the initial state for accumulation
   * @param process computes output value based on the input value and state
   * @return transformed query
   * */
  def statefulMapConcat[S: Tag, B: Tag](
    initialState: S
  )(process:      (S, Out) => (S, Iterable[B])
  ): Query[In, B] = new Query.StatefulMapConcat[In, Out, S, B](
    this,
    initialState,
    process,
    Tag[S].tag,
    nameHint = None
  )

  /**
   * Produces only unique values.
   *
   * @see [[deduplicateBy]]
   * @return the same query with only unique values
   * */
  def deduplicate: Query[In, Out] = deduplicateBy(identity[Out])

  /**
   * Produces only unique values based on the given deduplication key.
   *
   * Example:
   * {{{
   *   select[Person].deduplicateBy(_.name)
   * }}}
   *
   * @tparam K deduplication key type
   * @param f extracts deduplication key
   * @return the same query with only unique values
   * */
  def deduplicateBy[K: Tag](f: Out => K): Query[In, Out] =
    new Query.StatefulMapConcat[In, Out, Set[K], Out](
      this,
      initialState = Set.empty[K],
      process = { (keys, out) =>
        val key = f(out)
        if (keys.contains(key)) keys -> Nil
        else (keys + key)            -> List(out)
      },
      Tag[Set[K]].tag,
      nameHint = Some(s"DEDUPLICATE BY(${Tag[K].tag})")
    )

  /**
   * Symbolic alias for `union`
   * 
   * @see [[union]]
   * */
  final def ++[In2 <: In: Tag, Out0 >: Out: Tag](that: Query[In2, Out0]): Query[In2, Out0] =
    union(that)

  /**
   * Produces a query which emits results from both `this` and the specified new query.
   * 
   * @note the current implementation of union first produces all the results of `this` query,
   *       and then results of the specified query.
   *       But this behavior may change in future
   *
   * @tparam In2 inferred common input type for this query and the transformed result query
   * @tparam Out0 inferred common output type for this query and the transformed result query
   * @param that query to union with
   * @return union of `this` and `that` query
   * */
  def union[In2 <: In: Tag, Out0 >: Out: Tag](that: Query[In2, Out0]): Query[In2, Out0] =
    new Query.UnionQuery[In2, Out0](this, that)

  /**
   * Symbolic alias for `andThen`
   *
   * @see [[andThen]]
   * */
  final def >>>[Out0 >: Out: Tag, Out2: Tag](that: Query[From[Out0], Out2]): Query[In, Out2] =
    andThen(that)

  /**
   * Produces a query which uses `this` query input by producing output of the specified query.
   * Allows functional composition of multiple queries.
   * 
   * Example:
   * {{{
   *   val peopleSkills = select[Person].mapConcat(_.skills).distinct
   *   
   *   val programmingSkills = select[Skill].where(_.isProgrammingSkill)
   *   
   *   val result = peopleSkills >>> programmingSkills
   * }}}
   *
   * @tparam Out0 inferred common output type for this query and the transformed result query
   * @tparam Out2 new query output type
   * @param that query for which to feed `this` query output
   * @return a composition of `this` and `that` query
   * */
  def andThen[Out0 >: Out: Tag, Out2: Tag](that: Query[From[Out0], Out2]): Query[In, Out2] =
    new Query.AndThenQuery[In, Out0, Out2](this, that, Tag[Out0].tag)
}

object Query {

  final class Const[A: Tag](private[scalaql] val values: Iterable[A]) extends Query[Any, A] {
    override def explain: QueryExplain = {
      val truncatedValues = truncateToString(values)
      QueryExplain.Single(s"CONST($truncatedValues)")
    }
  }

  final class FromQuery[A: Tag] extends Query[From[A], A] {
    private[scalaql] val inputTag: LightTypeTag = Tag[A].tag

    override def explain: QueryExplain = QueryExplain.Single(s"FROM($inputTag)")
  }

  final class AliasedQuery[In: Tag, U: Tag](
    private[scalaql] val inputInfo:       LightTypeTag,
    private[scalaql] val inputAliasedTag: LightTypeTag,
    private[scalaql] val alias:           LightTypeTag)
      extends Query[From[In as U], In] {

    override def explain: QueryExplain = QueryExplain.Single(s"FROM($inputInfo AS ${alias.shortName})")
  }

  final class MapQuery[In: Tag, Out0, Out1: Tag](
    private[scalaql] val source:  Query[In, Out0],
    private[scalaql] val project: Out0 => Out1,
    private[scalaql] val outTag:  LightTypeTag)
      extends Query[In, Out1] {

    override def map[B: Tag](f: Out1 => B): Query[In, B] =
      new MapQuery[In, Out0, B](source, project andThen f, Tag[B].tag)

    override def explain: QueryExplain =
      QueryExplain.Continuation(source.explain, QueryExplain.Single(s"MAP($outTag)"))
  }

  final class FlatMapQuery[In: Tag, Out0, Out1: Tag](
    private[scalaql] val source:   Query[In, Out0],
    private[scalaql] val projectM: Out0 => Query[In, Out1],
    private[scalaql] val inTag:    LightTypeTag,
    private[scalaql] val outTag:   LightTypeTag,
    nameHint:                      Option[String] = None)
      extends Query[In, Out1] {

    override def map[B: Tag](f: Out1 => B): Query[In, B] =
      new FlatMapQuery[In, Out0, B](source, projectM(_).map(f), inTag, Tag[B].tag)

    override def where(p: Predicate[Out1]): Query[In, Out1] =
      new FlatMapQuery[In, Out0, Out1](source, projectM(_).where(p), inTag, outTag)

    override def explain: QueryExplain =
      QueryExplain.Continuation(
        source.explain,
        QueryExplain.Single(nameHint.getOrElse(s"FLATMAP($inTag => $outTag)"))
      )
  }

  final class WhereQuery[In: Tag, Out: Tag](
    private[scalaql] val source:     Query[In, Out],
    private[scalaql] val filterFunc: Predicate[Out],
    nameHint:                        Option[String] = None)
      extends Query[In, Out] {

    override def where(p: Predicate[Out]): Query[In, Out] =
      new WhereQuery[In, Out](source, out => filterFunc(out) && p(out))

    override def explain: QueryExplain =
      QueryExplain.Continuation(source.explain, QueryExplain.Single(nameHint.getOrElse("WHERE")))
  }

  final class CollectQuery[In: Tag, Out, Out1: Tag](
    private[scalaql] val source:      Query[In, Out],
    private[scalaql] val collectFunc: PartialFunction[Out, Out1],
    private[scalaql] val outTag:      LightTypeTag)
      extends Query[In, Out1] {

    override def collect[B: Tag](pf: PartialFunction[Out1, B]): Query[In, B] =
      new CollectQuery[In, Out, B](
        source,
        PartialFunctionAndThenCompat.andThen(collectFunc, pf),
        Tag[B].tag
      )

    override def where(p: Predicate[Out1]): Query[In, Out1] =
      new CollectQuery[In, Out, Out1](
        source,
        PartialFunctionAndThenCompat.andThen[Out, Out1, Out1](
          collectFunc,
          {
            case out1 if p(out1) => out1
          }
        ),
        outTag
      )

    override def explain: QueryExplain =
      QueryExplain.Continuation(source.explain, QueryExplain.Single(s"COLLECT($outTag)"))
  }

  final class WhereSubQuery[In: Tag, Out: Tag](
    private[scalaql] val source:    Query[In, Out],
    private[scalaql] val predicate: Out => QueryResult[In, Boolean])
      extends Query[In, Out] {

    override def where(p: Predicate[Out]): Query[In, Out] =
      new WhereSubQuery[In, Out](source, x => predicate(x).map(_ && p(x)))

    override def explain: QueryExplain =
      QueryExplain.Continuation(source.explain, QueryExplain.Single("WHERE SUBQUERY"))

    override def whereSubQuery[In2 <: In: Tag](p: Out => QueryResult[In2, Boolean]): Query[In2, Out] =
      new WhereSubQuery[In2, Out](
        source,
        out =>
          predicate(out).flatMap {
            case false => QueryResult.const(false)
            case true  => p(out)
          }
      )
  }

  final class StatefulMapConcat[In: Tag, Out, S, B: Tag](
    private[scalaql] val source:       Query[In, Out],
    private[scalaql] val initialState: S,
    private[scalaql] val process:      (S, Out) => (S, Iterable[B]),
    private[scalaql] val stateTag:     LightTypeTag,
    nameHint:                          Option[String])
      extends Query[In, B] {

    override def explain: QueryExplain = {
      val truncatedState = truncateToString(initialState)
      QueryExplain.Continuation(
        source.explain,
        QueryExplain.Single(
          nameHint.getOrElse(s"STATEFUL MAP CONCAT(initial: $stateTag = $truncatedState => ${Tag[B].tag})")
        )
      )
    }

    // TODO: optimize
  }

  final class Accumulate[In: Tag, Out, S, B: Tag](
    private[scalaql] val source:       Query[In, Out],
    private[scalaql] val initialState: S,
    private[scalaql] val modifyState:  (S, Out) => S,
    private[scalaql] val getResults:   S => Iterable[B],
    private[scalaql] val stateTag:     LightTypeTag)
      extends Query[In, B] {

    override def explain: QueryExplain = {
      val truncatedState = truncateToString(initialState)
      QueryExplain.Continuation(
        source.explain,
        QueryExplain.Single(s"ACCUMULATE(initial: $stateTag = $truncatedState => ${Tag[B].tag})")
      )
    }

    override def map[C: Tag](f: B => C): Query[In, C] =
      new Query.Accumulate[In, Out, S, C](
        source,
        initialState,
        modifyState,
        getResults(_).map(f),
        stateTag
      )

    override def mapConcat[C: Tag](f: B => Iterable[C]): Query[In, C] =
      new Query.Accumulate[In, Out, S, C](
        source,
        initialState,
        modifyState,
        getResults(_).flatMap(f),
        stateTag
      )

    override def collect[C: Tag](pf: PartialFunction[B, C]): Query[In, C] =
      new Query.Accumulate[In, Out, S, C](
        source,
        initialState,
        modifyState,
        getResults(_).collect(pf),
        stateTag
      )

    override def where(p: Predicate[B]): Query[In, B] =
      new Query.Accumulate[In, Out, S, B](
        source,
        initialState,
        modifyState,
        getResults(_).filter(p),
        stateTag
      )
  }

  final class OrderByQuery[In: Tag, Out: Tag, By](
    private[scalaql] val source:            Query[In, Out],
    private[scalaql] val orderBy:           Out => By,
    private[scalaql] val orderingTag:       Option[LightTypeTag]
  )(private[scalaql] implicit val ordering: Ordering[By])
      extends Query[In, Out] {

    override def explain: QueryExplain = {
      val byClause = orderingTag.fold(ifEmpty = "")(by => s" BY($by)")
      QueryExplain.Continuation(
        source.explain,
        QueryExplain.Single(s"ORDER$byClause")
      )
    }
  }

  /* Using alias to avoid implementing Liftable/ToExpr for the case class*/
  final type GroupingSetIndices = List[Int]
  final case class GroupingSetsDescription(
    values:           List[GroupingSetIndices],
    orderings:        List[Ordering[Any]],
    groupFillments:   Map[Int, Any => Any],
    defaultFillments: Map[Int, Any]) {

    val numGroups: Int = orderings.size
  }

  final case class GroupKeys(keys: Map[Int, Any]) {
    override lazy val hashCode: Int = MurmurHash3.orderedHash(
      keys.toList
        .sortBy { case (idx, _) => idx }
        .map { case (_, k) => k }
    )

    override def equals(obj: Any): Boolean = obj match {
      case rmk: GroupKeys => this.hashCode == rmk.hashCode
      case _              => false
    }

    lazy val size: Int = keys.size

    def apply(idx: Int): Any = keys(idx)
    def subgroups(sets: GroupingSetsDescription): List[(GroupKeys, Boolean)] =
      sets.values.map { set =>
        val sub = set.map { idx =>
          idx -> keys(idx)
        }.toMap

        val isSubtotal = sets.numGroups != sub.size

        GroupKeys(sub) -> isSubtotal
      }
  }

  final class AggregateQuery[In: Tag, Out0, Out1: Tag, Res: Tag](
    private[scalaql] val source:        Query[In, Out0],
    private[scalaql] val group:         Out0 => GroupKeys,
    private[scalaql] val groupingSets:  GroupingSetsDescription,
    private[scalaql] val agg:           QueryExpressionBuilder[Out0] => Aggregation.Of[Out0, Out1],
    private[scalaql] val groupByString: String,
    private[scalaql] val buildRes:      List[Any] => Res)
      extends Query[In, Res] {

    override def explain: QueryExplain =
      QueryExplain.Continuation(
        source.explain,
        QueryExplain.Continuation(
          QueryExplain.Single(groupByString),
          QueryExplain.Single(s"AGGREGATE(${Tag[Out1].tag})")
        )
      )
  }

  final class WindowQuery[In: Tag, Out, Res, B: Tag](
    private[scalaql] val source:            Query[In, Out],
    private[scalaql] val expressionBuilder: QueryExpressionBuilder[Out] => QueryExpression.Of[Out, Res],
    private[scalaql] val window:            Window[Out],
    private[scalaql] val flatten:           TupleFlatten.Of[(Out, Res), B])
      extends Query[In, B] {

    override def explain: QueryExplain = {
      val partitionBy = {
        val cols = tagsToString(window.__scalaql_window_partitionTags.reverse)
        s"PARTITION BY $cols"
      }

      val orderBy =
        if (window.__scalaql_window_orderTags.isEmpty) ""
        else {
          val cols = tagsToString(window.__scalaql_window_orderTags.reverse)
          s" ORDER BY $cols"
        }

      QueryExplain.Continuation(
        source.explain,
        QueryExplain.Single(s"WINDOW($partitionBy$orderBy => ${Tag[B].tag})")
      )
    }
  }

  final class InnerJoinPartiallyApplied[In <: From[?]: Tag, In2 <: From[?]: Tag, Out: Tag, Out2: Tag](
    left:     Query[In, Out],
    right:    Query[In2, Out2],
    joinType: InnerJoinType) {

    /**
     * Joins `this` query with the `that` query based on the given join condition.
     * If `that` query input type `B` differs from `this` input type `A`, 
     * then the resulting query type will be `Query[From[A] with From[B], (A, B)]`
     * 
     * @param f join condition
     * @return joined query
     * */
    def on(f: (Out, Out2) => Boolean): Query[In & In2, (Out, Out2)] =
      new InnerJoinedQuery[In, In2, Out, Out2](left, right, joinType, f)
  }

  private[scalaql] sealed abstract class InnerJoinType(override val toString: String)
  private[scalaql] case object InnerJoin extends InnerJoinType("INNER")
  private[scalaql] case object CrossJoin extends InnerJoinType("CROSS")

  sealed abstract class JoinedQuery[In <: From[?]: Tag, In2 <: From[?]: Tag, Out, Out2, Res: Tag]
      extends Query[In & In2, Res] {
    private[scalaql] val left: Query[In, Out]
    private[scalaql] val right: Query[In2, Out2]
  }

  final class InnerJoinedQuery[In <: From[?]: Tag, In2 <: From[?]: Tag, Out: Tag, Out2: Tag](
    private[scalaql] val left:     Query[In, Out],
    private[scalaql] val right:    Query[In2, Out2],
    private[scalaql] val joinType: InnerJoinType,
    private[scalaql] val on:       (Out, Out2) => Boolean)
      extends JoinedQuery[In, In2, Out, Out2, (Out, Out2)] {

    override def explain: QueryExplain =
      QueryExplain.Operation(
        left.explain,
        right.explain,
        s"$joinType JOIN"
      )
  }

  final class LeftJoinPartiallyApplied[In <: From[?]: Tag, In2 <: From[?]: Tag, Out: Tag, Out2: Tag](
    left:  Query[In, Out],
    right: Query[In2, Out2]) {

    /**
     * Left joins `this` query with the `that` query based on the given join condition.
     * If `that` query input type `B` differs from `this` input type `A`,
     * then the resulting query type will be `Query[From[A] with From[B], (A, Option[B])]`
     *
     * @param f join condition
     * @return joined query
     * */
    def on(f: (Out, Out2) => Boolean): Query[In & In2, (Out, Option[Out2])] =
      new LeftJoinedQuery[In, In2, Out, Out2](left, right, f)
  }

  final class LeftJoinedQuery[In <: From[?]: Tag, In2 <: From[?]: Tag, Out: Tag, Out2: Tag](
    private[scalaql] val left:  Query[In, Out],
    private[scalaql] val right: Query[In2, Out2],
    private[scalaql] val on:    (Out, Out2) => Boolean)
      extends JoinedQuery[In, In2, Out, Out2, (Out, Option[Out2])] {

    override def explain: QueryExplain =
      QueryExplain.Operation(
        left.explain,
        right.explain,
        "LEFT JOIN"
      )

  }

  final class UnionQuery[In: Tag, Out: Tag](
    private[scalaql] val left:  Query[In, Out],
    private[scalaql] val right: Query[In, Out])
      extends Query[In, Out] {

    override def where(p: Predicate[Out]): Query[In, Out] =
      new UnionQuery[In, Out](left.where(p), right.where(p))

    override def map[B: Tag](f: Out => B): Query[In, B] =
      new UnionQuery[In, B](left.map(f), right.map(f))

    override def collect[B: Tag](pf: PartialFunction[Out, B]): Query[In, B] =
      new UnionQuery[In, B](left.collect(pf), right.collect(pf))

    override def union[In2 <: In: Tag, Out0 >: Out: Tag](that: Query[In2, Out0]): Query[In2, Out0] =
      new UnionQuery[In2, Out0](left, right union that)

    override def whereSubQuery[In2 <: In: Tag](p: Out => QueryResult[In2, Boolean]): Query[In2, Out] =
      new UnionQuery[In2, Out](left.whereSubQuery(p), right.whereSubQuery(p))

    override def explain: QueryExplain =
      QueryExplain.Operation(
        left.explain,
        right.explain,
        "UNION"
      )
  }

  final class AndThenQuery[In0: Tag, OutA, OutB: Tag](
    private[scalaql] val left:    Query[In0, OutA],
    private[scalaql] val right:   Query[From[OutA], OutB],
    private[scalaql] val outATag: LightTypeTag)
      extends Query[In0, OutB] {

    override def explain: QueryExplain =
      QueryExplain.Operation(
        left.explain,
        right.explain,
        "AND THEN"
      )

    override def andThen[Out0 >: OutB: Tag, Out2: Tag](that: Query[From[Out0], Out2]): Query[In0, Out2] =
      new AndThenQuery[In0, OutA, Out2](
        left,
        right >>> that,
        outATag
      )

    override def map[B: Tag](f: OutB => B): Query[In0, B] =
      new AndThenQuery[In0, OutA, B](
        left,
        right.map(f),
        Tag[B].tag
      )

    override def where(p: Predicate[OutB]): Query[In0, OutB] =
      new AndThenQuery[In0, OutA, OutB](
        left,
        right.where(p),
        outATag
      )

    override def collect[B: Tag](pf: PartialFunction[OutB, B]): Query[In0, B] =
      new AndThenQuery[In0, OutA, B](
        left,
        right.collect(pf),
        Tag[B].tag
      )
  }

  private[scalaql] def tagsToString(tags: List[LightTypeTag]): String =
    tags.mkString("(", ", ", ")")

  private val DefaultStrLength: Int = 20
  private def truncateToString(value: Any): String = {
    val str = value.toString
    if (str.length <= DefaultStrLength) str
    else s"${str.take(DefaultStrLength)}... (${str.length - DefaultStrLength} symbols omitted)"
  }
}
