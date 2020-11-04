import sbt._
import sbt.Keys._
import sbt.nio.Keys._

import scala.Console
import scalafix.sbt.ScalafixPlugin.autoImport.{ scalafixDependencies, scalafixSemanticdb }
import explicitdeps.ExplicitDepsPlugin.autoImport._
import wartremover.WartRemover.autoImport._
import Dependencies._

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
      "-unchecked",                       // Enable add/itional warnings where generated code depends on assumptions.
      "-Xcheckinit",                      // Wrap field accessors to throw an exception on uninitialized access.
      "-Xfatal-warnings",                 // Fail the compilation if there are any warnings.
      "-opt-warnings",
      "-Xfuture",                         // Turn on future language features.

      // Warning Settings

      "-Ywarn-dead-code",                 // Warn when dead code is identified.
      "-Ywarn-extra-implicit",            // Warn when more than one implicit parameter section is defined.
      "-Ywarn-inaccessible",              // Warn about inaccessible types in method signatures.
      "-Ywarn-infer-any",                 // Warn when a type argument is inferred to be `Any`.
      "-Ywarn-nullary-override",          // Warn when non-nullary `def f()' overrides nullary `def f'.
      "-Ywarn-nullary-unit",              // Warn when nullary methods return Unit.
      "-Ywarn-numeric-widen",             // Warn when numerics are widened.
      "-Ywarn-value-discard",             // Warn when non-Unit expression results are unused.
      //      "-Wself-implicit",                // disabled due to false negatives
      "-Ywarn-unused:imports,_",
      "-Xlint",

      // Private Settings
      "-Yrangepos",

      // ??
      "-Yno-adapted-args",                // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
      "-Ypartial-unification"             // Enable partial unification in type constructor inference

      // format: on
    )

  private def stdSettings(prjName: String) = Seq(
    name := s"$prjName",
    organization := "com.agilogy",
    scalaVersion in ThisBuild := "2.12.12",
    Compile / console / scalacOptions --= Seq(
      "-deprecation",
      "-Xfatal-warnings",
      "-Xlint"
    ),
    libraryDependencies ++= {
      Seq(
        Ghik.silencerLibProvided,
        compilerPlugin(scalafixSemanticdb),
        Ghik.silencerCompilerPlugin
      )
    },
    parallelExecution in Test := true,
    autoAPIMappings := true,
    unusedCompileDependenciesFilter := moduleFilter() - moduleFilter(
      silencerLibProvided.organization,
      silencerLibProvided.name
    ),
    scalacOptions ++= compilerOptions ++ Seq("-P:silencer:checkUnused"),
    wartremoverWarnings ++= Warts.allBut(
      Wart.Any,
      Wart.Nothing,
      Wart.Equals,
      Wart.DefaultArguments,
      Wart.Overloading
    ),
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(paradise)
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
      Global / onChangedBuildSource := ReloadOnSourceChanges,
      unusedCompileDependenciesFilter -= moduleFilter(Ghik.ghik)
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
