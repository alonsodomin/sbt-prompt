import sbt._
import org.typelevel.scalacoptions.ScalacOptions

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val scala212 = "2.13.18"
lazy val scala3   = "3.10.0"

lazy val root = project
  .withId("sbt-prompt")
  .in(file("."))
  .settings(
    sbtPlugin := true,

    scalaVersion       := scala212,
    crossScalaVersions := Seq(scala212, scala3),

    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => (ThisBuild / sbtVersion).value
        case _      => "2.0.0"
      }
    },

    libraryDependencies += {
      val currentSbtVersion = (pluginCrossBuild / sbtBinaryVersion).value
      Defaults.sbtPluginExtra("com.github.sbt" % "sbt-git" % "2.2.0", currentSbtVersion, scalaBinaryVersion.value)
    },

    libraryDependencies += "org.slf4j" % "slf4j-nop" % "2.0.20",

    libraryDependencies += "org.scalameta" %% "munit" % "1.3.6" % Test,

    description          := "An SBT plugin for making your SBT prompt more awesome",
    organization         := "com.github.alonsodomin",
    licenses             := List("MIT" -> url("http://opensource.org/licenses/MIT")),
    startYear            := Some(2014),
    homepage             := Some(url("https://github.com/alonsodomin/sbt-prompt")),
    organizationHomepage := Some(url("https://github.com/alonsodomin/sbt-prompt")),
    developers           := List(
      Developer(
        "agemooij",
        "Age Mooij",
        "age.mooij@gmail.com",
        url("https://github.com/agemooij/")
      ),
      Developer(
        "alonsodomin",
        "Alonso Dominguez",
        "work@alonsodomin.me",
        url("https://github.com/alonsodomin")
      )
    ),

    versionScheme := Some("semver-spec"),

    scalacOptions ++= {
      scalaBinaryVersion.value match {
        case "2.12" =>
          Seq("-encoding", "utf8", "-deprecation", "-Xlog-reflective-calls", "-Ywarn-unused-import")
        case _ =>
          Seq("-encoding", "utf8", "-deprecation", "-Wunused:imports")
      }
    },

    semanticdbVersion := scalafixSemanticdb.revision,
    semanticdbEnabled := true,
    scalafmtOnCompile := true,
    scalafixOnCompile := true,

    tpolecatScalacOptions ++= Set(ScalacOptions.explain),
    Test / tpolecatExcludeOptions ++= Set(ScalacOptions.warnNonUnitStatement, ScalacOptions.warnValueDiscard),
    Test / console / tpolecatExcludeOptions ++= ScalacOptions.defaultConsoleExclude
  )
