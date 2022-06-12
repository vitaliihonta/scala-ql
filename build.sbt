import Dependencies._

val scala212 = "2.12.15"
val scala213 = "2.13.8"
val scala3   = "3.1.2"

val allScalaVersions = List(scala212, scala213, scala3)

ThisBuild / scalaVersion := scala213
ThisBuild / organization := "com.github.vitaliihonta"
ThisBuild / version      := "0.2.0"

val baseSettings = Seq(
  scalacOptions ++= Seq(
    "-language:implicitConversions",
    "-language:higherKinds",
    "-Xsource:3"
  ) ++ {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => Seq("-Ykind-projector")
      case _            => Nil
    }
  },
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => Nil
      case _            => Seq(Typelevel.kindProjector)
    }
  },
  ideSkipProject := scalaVersion.value == scala212
)

val crossCompileSettings: Seq[Def.Setting[_]] = {
  def crossVersionSetting(config: Configuration) =
    (config / unmanagedSourceDirectories) += {
      val sourceDir = (config / sourceDirectory).value
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, 1))            => sourceDir / "scala-3"
        case Some((2, n)) if n >= 13 => sourceDir / "scala-2.13+"
        case _                       => sourceDir / "scala-2.13-"
      }
    }

  Seq(
    crossVersionSetting(Compile),
    crossVersionSetting(Test)
  )
}

lazy val root = project
  .in(file("."))
  .settings(baseSettings)
  .settings(
    name            := "scala-ql-root",
    publishArtifact := false
  )
  .aggregate(
    `scala-ql`.projectRefs ++
      `scala-ql-csv`.projectRefs ++
      `scala-ql-json`.projectRefs ++
      examples.projectRefs: _*
  )

lazy val examples =
  projectMatrix
    .in(file("examples"))
    .dependsOn(`scala-ql`, `scala-ql-csv`, `scala-ql-json`)
    .settings(baseSettings)
    .settings(crossCompileSettings)
    .settings(
      publishArtifact := false,
      libraryDependencies ++= Seq(
      )
    )
    .jvmPlatform(scalaVersions = allScalaVersions)

lazy val `scala-ql` =
  projectMatrix
    .in(file("scala-ql"))
    .settings(baseSettings)
    .settings(crossCompileSettings)
    .settings(
      libraryDependencies ++= Seq(
        Reflect.izumi,
        Testing.scalatest,
        Testing.scalacheck
      ) ++ {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, n)) if n < 13 =>
            Seq(
              Typelevel.spire2_12
            )
          case _ =>
            Seq(
              Typelevel.spire
            )
        }
      }
    )
    .jvmPlatform(scalaVersions = allScalaVersions)

lazy val `scala-ql-json` =
  projectMatrix
    .in(file("scala-ql-json"))
    .settings(baseSettings)
    .settings(crossCompileSettings)
    .dependsOn(`scala-ql` % "compile->compile;test->test")
    .settings(
      libraryDependencies ++= Seq(
        Testing.scalatest,
        Testing.scalacheck,
        Json.circeCore,
        Json.circeParser
      )
    )
    .jvmPlatform(scalaVersions = allScalaVersions)

lazy val `scala-ql-csv` =
  projectMatrix
    .in(file("scala-ql-csv"))
    .settings(baseSettings)
    .settings(crossCompileSettings)
    .dependsOn(`scala-ql` % "compile->compile;test->test")
    .settings(
      libraryDependencies ++= Seq(
        Testing.scalatest,
        Testing.scalacheck,
        Csv.scalaCsv
      ) ++ {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((3, _)) =>
            Seq(
              Macros.magnoliaScala3
            )
          case _ =>
            Seq(
              Macros.magnoliaScala2,
              Macros.scalaMacros.value
            )
        }
      }
    )
    .jvmPlatform(scalaVersions = allScalaVersions)
