import sbt._

sonatypeProfileName  := "com.github.vitaliihonta"
organizationHomepage := Some(url("https://github.com/vitaliihonta"))
homepage             := Some(url("https://github.com/vitaliihonta"))
licenses := Seq(
  "Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
)
scmInfo := Some(
  ScmInfo(
    url(s"https://github.com/vitaliihonta/scala-ql"),
    s"scm:git:https://github.com/vitaliihonta/scala-ql.git",
    Some(s"scm:git:git@github.com:vitaliihonta/scala-ql.git")
  )
)
developers := List(
  Developer(
    id = "vitaliihonta",
    name = "Vitalii Honta",
    email = "vitalii.honta@gmail.com",
    url = new URL("https://github.com/vitaliihonta")
  )
)
sonatypeCredentialHost := "oss.sonatype.org"
