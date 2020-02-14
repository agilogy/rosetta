import BuildHelper._
import Dependencies._

globalSettings

def commonSettings = Seq(version := "0.1")

lazy val root = project
  .in(file("."))
  .settings(welcomeMessage)
  .aggregate(rosetta, rosettaCaliban, rosettaCirce)

lazy val rosetta = project.module
  .settings(
    libraryDependencies ++= Seq(
      catsCore
    )
  )
  .settings(commonSettings)
  .settings(Publish.publishSettings)

lazy val rosettaCaliban = project.module
  .dependsOn(rosetta)
  .settings(
    libraryDependencies ++= Seq(catsCore, caliban)
  )
  .settings(commonSettings)
  .settings(Publish.publishSettings)

lazy val rosettaCirce = project.module
  .dependsOn(rosetta)
  .settings(
    libraryDependencies ++= Seq(
      catsCore,
      Ghik.silencerLib
    ) ++ circeLibraries
  )
  .settings(commonSettings)
  .settings(munitTestSettings)
  .settings(Publish.publishSettings)