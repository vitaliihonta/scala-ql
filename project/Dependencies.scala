import sbt.Keys.scalaVersion

import sbt._

object Dependencies {

  object Reflect {
    val izumi = "dev.zio" %% "izumi-reflect" % "2.1.0"
  }

  object Typelevel {
    val spire2_12     = "org.typelevel" %% "spire" % "0.17.0"
    val spire         = "org.typelevel" %% "spire" % "0.18.0-M3"
    val kindProjector = compilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
  }

  object Csv {
    val scalaCsv = "com.github.tototoshi" %% "scala-csv" % "1.3.10"
  }

  object Json {
    val circeCore    = "io.circe" %% "circe-core"    % "0.14.2"
    val circeParser  = "io.circe" %% "circe-parser"  % "0.14.2"
    val circeGeneric = "io.circe" %% "circe-generic" % "0.14.2"
  }

  object Excel {
    val apachePoi = "org.apache.poi" % "poi-ooxml" % "5.2.2"
  }

  object Html {
    val scalatags = "com.lihaoyi" %% "scalatags" % "0.11.1"
  }

  object Utils {
    val commonsLang = "org.apache.commons" % "commons-lang3" % "3.12.0"
  }

  object Macros {
    val magnoliaScala3 = "com.softwaremill.magnolia1_3" %% "magnolia" % "1.1.3"

    val scalaMacros = Def.setting {
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
    }
    val magnoliaScala2 = "com.softwaremill.magnolia1_2" %% "magnolia" % "1.1.2"
  }

  object Testing {
    val scalatest  = "org.scalatest"  %% "scalatest"  % "3.2.12" % Test
    val scalacheck = "org.scalacheck" %% "scalacheck" % "1.16.0" % Test

    // Will be used only for testing some functionality
    val apacheSpark = "org.apache.spark" %% "spark-sql" % "3.2.1" % Test
  }
}
