import scalaql.syntax.ScalaqlSyntax

import spire.std.AnyInstances

/**
 * Welcome to ScalaQL - a simple statically typed query DSL for scala.
 * The library provides a composable Query - a description of your computations,
 * which you can then apply to multiple sources and write into multiple sinks.  
 *
 * @see [[https://scala-ql.vhonta.dev The documentation and examples]]
 * */
package object scalaql extends ScalaqlSyntax with AnyInstances
