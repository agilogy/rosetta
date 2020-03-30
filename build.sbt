import BuildHelper._
import Dependencies._

globalSettings

def commonSettings = Seq(version := "0.7")

lazy val root = project
  .in(file("."))
  .settings(welcomeMessage)
  .aggregate(rosetta, rosettaCirce)
  .settings(skip in publish := true)

lazy val rosetta = project.module
  .settings(
    libraryDependencies ++= Seq(
      catsKernel,
      catsCore
    )
  )
  .settings(commonSettings)
  .settings(Publish.publishSettings)
  .enablePlugins(spray.boilerplate.BoilerplatePlugin)

lazy val rosettaCirce = project.module
  .dependsOn(rosetta)
  .settings(
    libraryDependencies ++= Seq(
      catsCore,
      catsKernel,
      Ghik.silencerLib
    ) ++ circeLibraries
  )
  .settings(commonSettings)
  .settings(munitTestSettings)
  .settings(Publish.publishSettings)
  .enablePlugins(spray.boilerplate.BoilerplatePlugin)
