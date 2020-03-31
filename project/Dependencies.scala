import sbt._
import sbt.Keys.{ libraryDependencies, testFrameworks }

trait Circe {

  private val org          = "io.circe"
  private val circeVersion = "0.13.0"

  val circeCore          = org %% "circe-core"           % circeVersion
  val circeGeneric       = org %% "circe-generic"        % circeVersion
  val circeGenericExtras = org %% "circe-generic-extras" % circeVersion
  val circeParser        = org %% "circe-parser"         % circeVersion

  val circeLibraries = Seq(circeCore, circeParser)
}

trait Ghik {

  private val org             = "com.github.ghik"
  val ghik: String            = org
  private val silencerVersion = "1.4.4"

  val silencerLib         = org % "silencer-lib_2.12.10" % "1.4.4"
  val silencerLibProvided = org % "silencer-lib"         % silencerVersion % Provided cross CrossVersion.full
  val silencerCompilerPlugin = compilerPlugin(
    org % "silencer-plugin" % silencerVersion cross CrossVersion.full
  )
}

object Ghik extends Ghik

trait Ghostdogpr {

  private val org            = "com.github.ghostdogpr"
  private val calibanVersion = "0.4.1"

  val caliban       = org %% "caliban"        % calibanVersion
  val calibanHttp4s = org %% "caliban-http4s" % calibanVersion
  val calibanCats   = org %% "caliban-cats"   % calibanVersion

  val calibanServerLibraries = Seq(caliban, calibanHttp4s, calibanCats)
}

trait Typelevel {

  private val org         = "org.typelevel"
  private val catsVersion = "2.1.0"

  val catsKernel    = org %% "cats-kernel"    % catsVersion
  val catsCore      = org %% "cats-core"      % catsVersion
  val kindProjector = org %% "kind-projector" % "0.11.0" cross CrossVersion.full
  val simulacrum    = org %% "simulacrum"     % "1.0.0"
}

//trait Propensive {
//
//  private val org = "com.propensive"
//
//  val magnolia = org %% "magnolia" % "0.12.5"
//  val mercator = org %% "mercator" % "0.3.0"
//}

trait Nequissimus {

  private val org = "com.nequissimus"

  val sortImports = org %% "sort-imports" % "0.3.1"
}

trait Scalamacros {

  private val org = "org.scalamacros"

  val paradise = org % "paradise" % "2.1.0" cross CrossVersion.full
}

trait Scalameta {
  private val org = "org.scalameta"

  val munit = org %% "munit" % "0.4.5"

  val munitTestFramework = new TestFramework("munit.Framework")

  val munitTestSettings = Seq(
    libraryDependencies ++= Seq(munit % Test),
    testFrameworks ++= Seq(munitTestFramework)
  )
}

trait Scalatest {

  private val org = "org.scalatest"

  val scalaTest = org %% "scalatest" % "3.1.0" % Test
}

object Dependencies
    extends Circe
    with Ghik
    with Ghostdogpr
    with Nequissimus
    with Scalamacros
    with Scalameta
    with Scalatest
    with Typelevel
