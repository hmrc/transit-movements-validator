import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  private val catsVersion         = "2.9.0"
  private val boostrapPlayVersion = "7.12.0"

  val compile = Seq(
    "uk.gov.hmrc"               %% "bootstrap-backend-play-28"   % boostrapPlayVersion,
    "org.typelevel"             %% "cats-core"                   % catsVersion,
    "com.networknt"              % "json-schema-validator"       % "1.0.70" exclude ("org.slf4j", "slf4j-api"),
    "com.fasterxml.jackson.core" % "jackson-databind"            % "2.12.6"
  )

  val test = Seq(
    "org.mockito"             % "mockito-core"           % "3.9.0",
    "org.scalatest"          %% "scalatest"              % "3.2.12",
    "org.typelevel"          %% "cats-core"              % catsVersion,
    "com.typesafe.play"      %% "play-test"              % current,
    "org.pegdown"             % "pegdown"                % "1.6.0",
    "org.scalatestplus.play" %% "scalatestplus-play"     % "5.1.0",
    "org.scalatestplus"      %% "mockito-3-2"            % "3.1.2.0",
    "org.scalacheck"         %% "scalacheck"             % "1.16.0",
    "com.github.tomakehurst"  % "wiremock-standalone"    % "2.27.2",
    "org.typelevel"          %% "discipline-scalatest"   % "2.1.5",
    "com.vladsch.flexmark"    % "flexmark-all"           % "0.62.2",
    "com.typesafe.akka"      %% "akka-stream-testkit"    % "2.6.20",
    "uk.gov.hmrc"            %% "bootstrap-test-play-28" % boostrapPlayVersion
  ).map(_ % "test, it")
}
