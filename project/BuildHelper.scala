import sbt._
import sbt.Keys._
import sbt.nio.Keys._

import scala.Console
import scalafix.sbt.ScalafixPlugin.autoImport.{ scalafixDependencies, scalafixSemanticdb }
import wartremover.WartRemover.autoImport._
import Dependencies._
import explicitdeps.ExplicitDepsPlugin.autoImport.unusedCompileDependenciesFilter

// http://eed3si9n.com/stricter-scala-with-xlint-xfatal-warnings-and-scalafix
// https://tpolecat.github.io/2017/04/25/scalac-flags.html
object BuildHelper {

  private val compilerOptions: Seq[String] =
    Seq(
      // format: off

      // Standard Settings
      "-deprecation",                     // Emit warning and location for usages of deprecated APIs.
      "-encoding",
      "utf-8",                            // Specify character encoding used by source files.
      "-explaintypes",                    // Explain type errors in more detail.
      "-feature",                         // Emit warning and location for usages of features that should be imported explicitly.
      "-language:existentials",           // Existential types (besides wildcard types) can be written and inferred
      "-language:higherKinds",            // Allow higher-kinded types
      "-Xcheckinit",                      // Wrap field accessors to throw an exception on uninitialized access.
      "-Xfatal-warnings",                 // Fail the compilation if there are any warnings.
      "-opt-warnings",
      "-Xfuture",                         // Turn on future language features.
      "-Ymacro-annotations",

      // Warning Settings

      "-unchecked",                       // Enable additional warnings where generated code depends on assumptions.
      "-Wdead-code",                      // Warn when dead code is identified.
      "-Wextra-implicit",                 // Warn when more than one implicit parameter section is defined.
      "-Wnumeric-widen",                  // Warn when numerics are widened.
      "-Ywarn-value-discard",             // Warn when non-Unit expression results are unused.
      //"-Wself-implicit",                // disabled due to false negatives
      "-Wunused:implicits",
      "-Wunused:imports",
      "-Wunused:locals",
      "-Wunused:params",
      "-Wunused:patvars",
      "-Wunused:privates",
      "-Wvalue-discard",
      "-Xlint",
      
  // Private Settings
      "-Yrangepos"                        // Use range positions for syntax trees.

      // format: on
    )

  private def stdSettings(prjName: String) = Seq(
    name := s"$prjName",
    organization := "com.agilogy",
    scalaVersion in ThisBuild := "2.13.3",
    semanticdbEnabled := true,                        // enable SemanticDB
    semanticdbVersion := scalafixSemanticdb.revision, // use Scalafix compatible version
    scalacOptions ++= compilerOptions ++ Seq("-P:silencer:checkUnused"),
    Compile / console / scalacOptions --= Seq(
      "-deprecation",
      "-Xfatal-warnings",
      "-Werror",
      "-Wdead-code",
      "-Wunused:imports",
      "-Ywarn-dead-code",
      "-Xlint"
    ),
    parallelExecution in Test := true,
    autoAPIMappings := true,
    libraryDependencies ++= {
      Seq(
        Ghik.silencerLibProvided,
        Ghik.silencerCompilerPlugin
      )
    },
    unusedCompileDependenciesFilter := moduleFilter() - moduleFilter(
      silencerLibProvided.organization,
      silencerLibProvided.name
    ),
    wartremoverWarnings ++= Warts.allBut(
      Wart.Any,
      Wart.Nothing,
      Wart.Equals,
      Wart.DefaultArguments,
      Wart.Overloading
    ),
    addCompilerPlugin(kindProjector)
  )

  def welcomeMessage = onLoadMessage := {

    def item(text: String): String =
      s"${Console.GREEN}▶ ${Console.CYAN}$text${Console.RESET}"

    s"""|
        |Useful sbt tasks:
        |${item("build")} - Format, check & test: prepare + depsCheck + test
        |${item("prepare")} - Format source code: fix + fmt
        |${item("check")} - Statically checks the build: fixCheck + fmtCheck + depsCheck
        |${item("fix")} - Runs scalafix
        |${item("fixCheck")} - Checks scalafix
        |${item("fmt")} - Runs scalafmt
        |${item("fmtCheck")} - Checks scalafmt
        |${item("depsCheck")} - Check unused and declared dependencies and used but undeclared dependencies
      """.stripMargin
  }

  def globalSettings: Seq[Def.Setting[_]] =
    Seq(
      scalafixDependencies in ThisBuild += sortImports,
      Global / onChangedBuildSource := ReloadOnSourceChanges
    ) ++
      addCommandAlias("build", "prepare; depsCheck; test") ++
      addCommandAlias("prepare", "fix; fmt") ++
      addCommandAlias("fix", "all compile:scalafix test:scalafix") ++
      addCommandAlias(
        "fixCheck",
        "; compile:scalafix --check ; test:scalafix --check"
      ) ++
      addCommandAlias("fmt", "all root/scalafmtSbt root/scalafmtAll") ++
      addCommandAlias(
        "fmtCheck",
        "all root/scalafmtSbtCheck root/scalafmtCheckAll"
      ) ++
      addCommandAlias(
        "depsCheck",
        "; unusedCompileDependenciesTest; undeclaredCompileDependenciesTest"
      ) ++
      addCommandAlias("check", "fixCheck; fmtCheck; depsCheck")

  implicit class ModuleHelper(p: Project) {
    def module: Project = p.in(file(p.id)).settings(stdSettings(p.id))
  }

}
