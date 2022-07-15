import Dependencies._

val scala212 = "2.12.15"
val scala213 = "2.13.8"
val scala3   = "3.1.2"

val allScalaVersions = List(scala212, scala213, scala3)

ThisBuild / scalaVersion  := scala213
ThisBuild / organization  := "dev.vhonta"
ThisBuild / version       := "0.2.0-RC4"
ThisBuild / versionScheme := Some("early-semver")

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository     := "https://s01.oss.sonatype.org/service/local"

val publishSettings = Seq(
  publishTo            := sonatypePublishToBundle.value,
  publishMavenStyle    := true,
  organizationHomepage := Some(url("https://vhonta.dev")),
  homepage             := Some(url("https://vhonta.dev")),
  licenses := Seq(
    "Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
  ),
  scmInfo := Some(
    ScmInfo(
      url(s"https://github.com/vitaliihonta/scala-ql"),
      s"scm:git:https://github.com/vitaliihonta/scala-ql.git",
      Some(s"scm:git:git@github.com:vitaliihonta/scala-ql.git")
    )
  ),
  developers := List(
    Developer(
      id = "vitaliihonta",
      name = "Vitalii Honta",
      email = "vitalii.honta@gmail.com",
      url = new URL("https://github.com/vitaliihonta")
    )
  )
)

lazy val baseProjectSettings = Seq(
  scalacOptions ++= {
    val baseOptions = Seq(
      "-language:implicitConversions",
      "-language:higherKinds"
    )
    val crossVersionOptions = CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _))           => Seq("-Ykind-projector")
      case Some((2, y)) if y < 13 => Seq("-Ypartial-unification", "-Xsource:3")
      case Some((2, _))           => Seq("-Xsource:3")
    }
    baseOptions ++ crossVersionOptions
  },
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) =>
        List(Typelevel.kindProjector)
      case _ => Nil
    }
  },
  ideSkipProject := scalaVersion.value == scala212
)

val crossCompileSettings: Seq[Def.Setting[_]] = {
  def crossVersionSetting(config: Configuration) =
    (config / unmanagedSourceDirectories) ++= {
      val sourceDir = (config / sourceDirectory).value
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _))            => List(sourceDir / "scala-3")
        case Some((2, n)) if n >= 13 => List(sourceDir / "scala-2", sourceDir / "scala-2.13+")
        case _                       => List(sourceDir / "scala-2", sourceDir / "scala-2.13-")
      }
    }

  Seq(
    crossVersionSetting(Compile),
    crossVersionSetting(Test)
  )
}

val coverageSettings = Seq(
//  Keys.fork in org.jacoco.core.
  jacocoAggregateReportSettings := JacocoReportSettings(
    title = "ScalaQL Coverage Report",
    subDirectory = None,
    thresholds = JacocoThresholds(),
    formats = Seq(JacocoReportFormats.ScalaHTML, JacocoReportFormats.XML), // note XML formatter
    fileEncoding = "utf-8"
  )
)

val baseSettings    = baseProjectSettings
val baseLibSettings = baseSettings ++ publishSettings ++ coverageSettings

lazy val root = project
  .in(file("."))
  .settings(baseSettings, noPublishSettings, unidocSettings)
  .settings(
    name := "scala-ql-root"
  )
  .aggregate(
    `scala-ql`.projectRefs ++
      `scala-ql-csv`.projectRefs ++
      `scala-ql-json`.projectRefs ++
      `scala-ql-excel`.projectRefs ++
      `scala-ql-html`.projectRefs ++
      `integration-tests`.projectRefs: _*
  )
  .aggregate(
    docs
  )
  .enablePlugins(ScalaUnidocPlugin)

lazy val docs = project
  .in(file("docs"))
  .settings(
    name := "scala-ql-docs",
    baseSettings,
    mdocSettings,
    noPublishSettings,
    scalaVersion := scala213,
    libraryDependencies ++= Seq(
      Json.circeGeneric
    )
  )
  .dependsOn(
    `scala-ql`.jvm(scala213),
    `scala-ql-csv`.jvm(scala213),
    `scala-ql-json`.jvm(scala213),
    `scala-ql-excel`.jvm(scala213),
    `scala-ql-html`.jvm(scala213)
  )
  .enablePlugins(MdocPlugin, DocusaurusPlugin)

lazy val coverage = project
  .in(file("./.coverage"))
  .settings(baseSettings, coverageSettings, noPublishSettings)
  .aggregate(
    `scala-ql`.jvm(scala213),
    `scala-ql-csv`.jvm(scala213),
    `scala-ql-json`.jvm(scala213),
    `scala-ql-excel`.jvm(scala213),
    `scala-ql-html`.jvm(scala213),
    `integration-tests`.jvm(scala213)
  )

lazy val `integration-tests` =
  projectMatrix
    .in(file("integration-tests"))
    .dependsOn(
      `scala-ql`      % "compile->compile;test->test",
      `scala-ql-csv`  % "compile->compile;test->test",
      `scala-ql-json` % "compile->compile;test->test"
    )
    .settings(
      baseSettings,
      noPublishSettings,
      crossCompileSettings,
      libraryDependencies ++= Seq(
        Testing.scalatest,
        Testing.scalacheck
      )
    )
    .jvmPlatform(scalaVersions = allScalaVersions)

lazy val `scala-ql` =
  projectMatrix
    .in(file("scala-ql"))
    .settings(baseLibSettings)
    .settings(crossCompileSettings)
    .settings(
      libraryDependencies ++= Seq(
        Reflect.izumi,
        Testing.scalatest,
        Testing.scalacheck,
        Utils.commonsLang
      ) ++ {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((3, _)) =>
            Seq(
              Typelevel.spire,
              Macros.magnoliaScala3
            )
          case Some((2, n)) if n >= 13 =>
            Seq(
              Typelevel.spire,
              Macros.magnoliaScala2,
              Macros.scalaMacros.value
            )
          case _ =>
            Seq(
              Typelevel.spire2_12,
              Macros.magnoliaScala2,
              Macros.scalaMacros.value
            )
        }
      }
    )
    .jvmPlatform(scalaVersions = allScalaVersions)

lazy val `scala-ql-json` =
  projectMatrix
    .in(file("scala-ql-json"))
    .settings(baseLibSettings)
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
    .settings(baseLibSettings)
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

lazy val `scala-ql-excel` =
  projectMatrix
    .in(file("scala-ql-excel"))
    .settings(baseLibSettings)
    .settings(crossCompileSettings)
    .dependsOn(`scala-ql` % "compile->compile;test->test")
    .settings(
      libraryDependencies ++= Seq(
        Testing.scalatest,
        Testing.scalacheck,
        Excel.apachePoi
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

lazy val `scala-ql-html` =
  projectMatrix
    .in(file("scala-ql-html"))
    .settings(baseLibSettings)
    .settings(crossCompileSettings)
    .dependsOn(`scala-ql` % "compile->compile;test->test")
    .settings(
      libraryDependencies ++= Seq(
        Testing.scalatest,
        Testing.scalacheck,
        Html.scalatags
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

// MDOC
lazy val mdocSettings = Seq(
  mdocIn := file("docs/src/main/mdoc"),
  mdocVariables := Map(
    "VERSION"      -> version.value,
    "ORGANIZATION" -> organization.value,
    "EMAIL"        -> developers.value.head.email
  ),
  crossScalaVersions := Seq(scalaVersion.value)
)

lazy val unidocSettings = Seq(
  cleanFiles += (ScalaUnidoc / unidoc / target).value,
  ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
    `scala-ql`.jvm(scala213),
    `scala-ql-csv`.jvm(scala213),
    `scala-ql-json`.jvm(scala213),
    `scala-ql-excel`.jvm(scala213),
    `scala-ql-html`.jvm(scala213)
  ),
  ScalaUnidoc / unidoc / target := (LocalRootProject / baseDirectory).value / "website" / "static" / "api",
  ScalaUnidoc / unidoc / scalacOptions ++= Seq(
    "-sourcepath",
    (LocalRootProject / baseDirectory).value.getAbsolutePath,
    "-doc-title",
    "Scala QL",
    "-doc-version",
    s"v${(ThisBuild / version).value}"
  )
)

val updateSiteVariables = taskKey[Unit]("Update site variables")
ThisBuild / updateSiteVariables := {
  val file =
    (ThisBuild / baseDirectory).value / "website" / "variables.js"

  val variables =
    Map[String, String](
      "organization"           -> (ThisBuild / organization).value,
      "latestVersion"          -> (ThisBuild / version).value,
      "downloadReportsBaseUrl" -> "https://scala-ql.vhonta.dev/assets"
    )

  val fileHeader =
    "// Generated by sbt. Do not edit directly."

  val fileContents =
    variables.toList
      .sortBy { case (key, _) => key }
      .map { case (key, value) => s"  $key: '$value'" }
      .mkString(s"$fileHeader\nmodule.exports = {\n", ",\n", "\n};\n")

  IO.write(file, fileContents)
}

// MISC
lazy val noPublishSettings =
  publishSettings ++ Seq(
    publish / skip  := true,
    publishArtifact := false
  )

Global / excludeLintKeys ++= Set(
  ideSkipProject,
  jacocoAggregateReportSettings
)
