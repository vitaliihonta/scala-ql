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
    libraryUsageValidityCheck[A]

    val fieldName = extractSelectorField(f.tree)
      .map(_.toString)
      .getOrElse(
        error(s"Expected a field selector to be passed (as instance.field1), got $f")
      )

    use(c.prefix.tree, fieldName)
  }

  protected def libraryUsageValidityCheck[A: WeakTypeTag](implicit builderTT: WeakTypeTag[Builder[A]]): Unit = {
    if (!(c.prefix.tree.tpe =:= weakTypeOf[Builder[A]])) {
      error("Invalid library usage! Refer to documentation")
    }
    val A           = weakTypeOf[A].dealias
    val tpe         = A.typeSymbol
    val isCaseClass = tpe.isClass && tpe.asClass.isCaseClass
    if (!isCaseClass) {
      error(s"Expected $A to be a case class")
    }
  }
}
