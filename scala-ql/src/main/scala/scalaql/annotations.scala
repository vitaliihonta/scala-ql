package scalaql

import scala.annotation.StaticAnnotation

/**
 * Indicates than annotated function/method should not be used outside of `scalaql` itself
 * */
class internalApi extends StaticAnnotation

/**
 * Indicates than annotated function/method/class may change in future
 * */
class unstableApi extends StaticAnnotation

/**
 * Indicates than annotated class/trait should not be inherited by any class except of `scalaql` itself.
 * */
class forbiddenInheritance extends StaticAnnotation
