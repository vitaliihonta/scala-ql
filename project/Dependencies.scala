import sbt._

object Dependencies {

  object Reflect {
    val izumi = "dev.zio" %% "izumi-reflect" % "1.1.1"
  }

  object Typelevel {
    val spire         = "org.typelevel" %% "spire" % "0.17.0"
    val kindProjector = compilerPlugin("org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full)
  }

  object Testing {
    val scalatest           = "org.scalatest"              %% "scalatest"                 % "3.2.5"  % Test
    val scalacheck          = "org.scalacheck"             %% "scalacheck"                % "1.14.1" % Test
    val scalacheckShapeless = "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.3"  % Test
  }
}
