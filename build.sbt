import Dependencies._

val scala213 = "2.13.5"
val scala212 = "2.12.13"

val allScalaVersions = List(scala212, scala213)

scalaVersion := scala213

ThisBuild / organization := "com.github.vitaliihonta"
ThisBuild / version :="0.1.0"

val baseSettings = Seq(
  scalacOptions ++= Seq(
    "-language:implicitConversions",
    "-language:higherKinds"
  ),
  libraryDependencies ++= Seq(
    Typelevel.kindProjector
  ),
  ideSkipProject := scalaVersion.value == scala212
)

val crossCompileSettings: Seq[Def.Setting[_]] = {
  def crossVersionSetting(config: Configuration) =
    (config / unmanagedSourceDirectories) += {
      val sourceDir = (config / sourceDirectory).value
      CrossVersion.partialVersion(scalaVersion.value) match {
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
    name := "scala-ql-root",
    publishArtifact := false
  )
  .aggregate(
    `scala-ql`.projectRefs ++
      examples.projectRefs: _*
  )

lazy val examples =
  projectMatrix
    .in(file("examples"))
    .dependsOn(`scala-ql`)
    .settings(baseSettings)
    .settings(crossCompileSettings)
    .settings(
      publishArtifact := false
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
        Typelevel.spire,
        Testing.scalatest,
        Testing.scalacheck,
        Testing.scalacheckShapeless
      )
    )
    .jvmPlatform(scalaVersions = allScalaVersions)
