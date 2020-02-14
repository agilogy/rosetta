import bintray.BintrayKeys._
import sbt.{ url, ThisBuild }
import sbt.Keys.licenses

object Publish {
  lazy val publishSettings = Seq(
    bintrayRepository := "scala",
    bintrayReleaseOnPublish in ThisBuild := false,
    bintrayOrganization := Some("agilogy"),
    bintrayPackageLabels := Seq("scala"),
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
  )
}
