import sbt._
import Keys._

import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._

import java.util.Properties
import java.nio.file._
import java.io.FileInputStream

/*
This code is shared between the build and the "build-build".
it allows us to use the api project at the build level
to generate the docker image for the different
configuration matrix
 */
object SbtShared {
  object ScalaVersions {
    val latest210 = "2.10.7"
    val latest211 = "2.11.12"
    val latest212 = "2.12.10"
    val latest213 = "2.13.1"
    val latestDotty = "0.21.0-RC1"
    val js = latest212
    val sbt = latest212
    val jvm = latest212
    val cross = List(latest210, latest211, latest212, latest213, js, sbt, jvm).distinct
  }

  val latestScalaJs = "0.6.31"

  val sbtVersion = "1.3.7"
  val distSbtVersion = sbtVersion

  val runtimeProjectName = "runtime-scala"

  def gitIsDirty(): Boolean = {
    import sys.process._
    true
  }

  def gitHash(): String = {
    import sys.process._
    if (!sys.env.contains("CI")) {

      val indexState =
        if (gitIsDirty()) "-dirty"
        else ""

      "XXX" + indexState
    } else "CI"
  }

  val gitHashNow = gitHash()
  val gitIsDirtyNow = gitIsDirty()

  val versionNow = {
    val base = "0.29.0"
    if (gitIsDirtyNow)
      base + "-SNAPSHOT"
    else {
      val hash = gitHashNow
      s"$base+$hash"
    }
  }
  val versionRuntime = "1.0.0-SNAPSHOT"

  lazy val baseSettings = Seq(
    // skip scaladoc
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in packageDoc := false,
    publishArtifact in packageSrc := false,
    sources in (Compile, doc) := Seq.empty,
    parallelExecution in Test := false,
    scalacOptions ++= {
      val scalaV = scalaVersion.value

      val base =
        Seq(
          "-deprecation",
          "-encoding",
          "UTF-8",
          "-feature",
          "-unchecked"
        )

      if (scalaV == ScalaVersions.latest210) base
      else {
        base ++ Seq(
          "-Yrangepos",
        )
      }

    },
    console := (console in Test).value,
    scalacOptions in (Test, console) -= "-Ywarn-unused-import",
    scalacOptions in (Compile, consoleQuick) -= "-Ywarn-unused-import"
  ) ++ orgSettings

  lazy val orgSettings = Seq(
    organization := "org.scastie",
    version := versionNow
  )

  val baseJsSettings = Seq(
    scalacOptions += "-P:scalajs:sjsDefinedByDefault",
    test := {},
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.8",
  )

  private def readSbtVersion(base: Path): String = {
    val sbtPropertiesFileName = "build.properties"
    val projectFolder = "project"
    val to = Paths.get(projectFolder, sbtPropertiesFileName)
    val guess1 = base.resolve(to)
    val guess2 = base.getParent.resolve(to)
    val sbtPropertiesFile =
      if (Files.isRegularFile(guess1)) guess1
      else if (Files.isRegularFile(guess2)) guess2
      else {
        sys.error(
          s"cannot find $sbtPropertiesFileName in $guess1 and $guess2"
        )
      }
    val prop = new Properties() {
      new FileInputStream(sbtPropertiesFile.toFile) {
        load(this)
        close()
      }
    }
    val res = prop.getProperty("sbt.version")
    assert(res != null)
    res
  }

  /* api is for the communication between sbt <=> server <=> frontend */
  lazy val apiProject = Project(id = "api", base = file("api"))
    .settings(apiSettings)
    .settings(baseJsSettings)
    .enablePlugins(org.scalajs.sbtplugin.ScalaJSPlugin, BuildInfoPlugin)

  private def apiSettings = {
    baseSettings ++ List(
      scalaVersion := "2.12.10",
      name := "api",
      libraryDependencies += {
        scalaVersion.value match {
          case v if v.startsWith("2.10") =>
            "com.typesafe.play" %%% "play-json" % "2.6.9"
          case v if v.startsWith("2.11") =>
            "com.typesafe.play" %%% "play-json" % "2.7.4"
          case _ =>
            "com.typesafe.play" %%% "play-json" % "2.8.1"
        }
      },
      buildInfoKeys := Seq[BuildInfoKey](
        organization,
        "runtimeProjectName" -> runtimeProjectName,
        "versionRuntime" -> versionRuntime,
        "latest210" -> ScalaVersions.latest210,
        "latest211" -> ScalaVersions.latest211,
        "latest212" -> ScalaVersions.latest212,
        "latest213" -> ScalaVersions.latest213,
        "latestDotty" -> ScalaVersions.latestDotty,
        "jsScalaVersion" -> ScalaVersions.js,
        "defaultScalaJsVersion" -> latestScalaJs,
        "sbtVersion" -> readSbtVersion((baseDirectory in ThisBuild).value.toPath),
      ),
      buildInfoPackage := "com.olegych.scastie.buildinfo",
    )
  }

}
