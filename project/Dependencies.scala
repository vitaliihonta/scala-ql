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

  object Testing {
    val scalatest  = "org.scalatest"  %% "scalatest"  % "3.2.12" % Test
    val scalacheck = "org.scalacheck" %% "scalacheck" % "1.16.0" % Test
  }
}
