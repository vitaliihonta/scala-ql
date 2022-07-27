package scalaql.internal

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

abstract class BaseTypeCheckedBuilder[Builder[_]](override val c: blackbox.Context)(prefix: String)
    extends MacroUtils(c)(prefix) {

  import c.universe.*

  protected def builderStepImpl[A: WeakTypeTag, B: WeakTypeTag](
    f:                  Expr[A => B]
  )(use:                (Tree, String) => Tree
  )(implicit builderTT: WeakTypeTag[Builder[A]]
  ): Tree = {
    checkValidBuilderUsage[A]

    val fieldName = extractSelectorField(f.tree)
      .map(_.toString)
      .getOrElse(
        error(
          FatalExceptions.macroDslErrorMessage(
            s"expected a field selector to be passed (such as instance.field1), got ${f.tree}"
          )
        )
      )

    use(c.prefix.tree, fieldName)
  }

  protected def checkValidBuilderUsage[A: WeakTypeTag](implicit builderTT: WeakTypeTag[Builder[A]]): Unit = {
    if (!(c.prefix.tree.tpe =:= weakTypeOf[Builder[A]])) {
      error(
        FatalExceptions.invalidLibraryUsageMessage(
          s"Builder methods called outside of the builder, in ${c.prefix.tree}"
        )
      )
    }
    val A           = weakTypeOf[A].dealias
    val tpe         = A.typeSymbol
    val isCaseClass = tpe.isClass && tpe.asClass.isCaseClass
    if (!isCaseClass) {
      error(
        FatalExceptions.macroDslErrorMessage(
          s"expected builder type $A to be a case class"
        )
      )
    }
  }
}
