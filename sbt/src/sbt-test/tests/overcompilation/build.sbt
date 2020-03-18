import SbtShared._

val webpackDir = Def.setting {
  (baseDirectory in ThisProject).value / "webpack"
}

val webpackDevConf = Def.setting {
  Some(webpackDir.value / "webpack-dev.config.js")
}

val webpackProdConf = Def.setting {
  Some(webpackDir.value / "webpack-prod.config.js")
}

import sbt.internal.inc.Analysis
import complete.DefaultParsers._

// Reset compiler iterations, necessary because tests run in batch mode
val recordPreviousIterations = taskKey[Unit]("Record previous iterations.")
val checkIterations = inputKey[Unit]("Verifies the accumulated number of iterations of incremental compilation.")

lazy val client = project
  .settings(baseSettings)
  .settings(baseJsSettings)
  .settings(
    version in webpack := "3.5.5",
    scalaVersion := "2.12.10",
    version in startWebpackDevServer := "2.7.1",
    webpackConfigFile in fastOptJS := webpackDevConf.value,
    webpackConfigFile in fullOptJS := webpackProdConf.value,
    webpackMonitoredDirectories += (resourceDirectory in Compile).value,
    webpackResources := webpackDir.value * "*.js",
    includeFilter in webpackMonitoredFiles := "*",
    useYarn := true,
    webpackBundlingMode in fastOptJS := BundlingMode.LibraryOnly(),
    webpackBundlingMode in fullOptJS := BundlingMode.Application,
    test := {},
    npmDependencies in Compile ++= Seq(
      "codemirror" -> "5.50.0",
      "firacode" -> "1.205.0",
      "font-awesome" -> "4.7.0",
      "raven-js" -> "3.11.0",
      "react" -> "16.7.0",
      "react-dom" -> "16.7.0",
      "typeface-roboto-slab" -> "0.0.35"
    ),
    npmDevDependencies in Compile ++= Seq(
      "compression-webpack-plugin" -> "1.0.0",
      "clean-webpack-plugin" -> "0.1.16",
      "css-loader" -> "0.28.5",
      "extract-text-webpack-plugin" -> "3.0.0",
      "file-loader" -> "0.11.2",
      "html-webpack-plugin" -> "2.30.1",
      "node-sass" -> "4.5.3",
      "resolve-url-loader" -> "2.1.0",
      "sass-loader" -> "6.0.6",
      "style-loader" -> "0.18.2",
      "uglifyjs-webpack-plugin" -> "0.4.6",
      "webpack-merge" -> "4.1.0"
    ),
    libraryDependencies ++= Seq(
      "com.github.japgolly.scalajs-react" %%% "extra" % "1.5.0",
    ),
    recordPreviousIterations := {
      val log = streams.value.log
      CompileState.previousIterations = {
        val previousAnalysis = (previousCompile in Compile).value.analysis.asScala
        previousAnalysis match {
          case None =>
            log.info("No previous analysis detected")
            0
          case Some(a: Analysis) => a.compilations.allCompilations.size
          case x => sys.error(x.toString)
        }
      }
    },
    checkIterations := {
      val expected: Int = (Space ~> NatBasic).parsed
      val actual: Int = ((compile in Compile).value match { case a: Analysis => a.compilations.allCompilations.size }) - CompileState.previousIterations
      assert(expected == actual, s"Expected $expected compilations, got $actual")
    },
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(api)

lazy val api = apiProject

