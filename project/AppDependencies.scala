import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val catsVersion = "2.7.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "5.24.0",
    "org.typelevel"           %% "cats-core"                  % catsVersion
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % "5.24.0"            % "test, it",
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.36.8"            % "test, it"
  )
}
