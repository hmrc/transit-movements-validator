import sbt.*

object AppDependencies {

  private val catsVersion         = "2.9.0"
  private val boostrapPlayVersion = "9.19.0"
  private val pekkoVersion        = "1.0.1"

  val compile = Seq(
    "uk.gov.hmrc"               %% "bootstrap-backend-play-30"       % boostrapPlayVersion,
    "org.typelevel"             %% "cats-core"                       % catsVersion,
    "com.networknt"              % "json-schema-validator"           % "1.0.70" exclude ("org.slf4j", "slf4j-api"),
    "com.fasterxml.jackson.core" % "jackson-databind"                % "2.12.6",
    "org.apache.pekko"          %% "pekko-connectors-xml"            % pekkoVersion,
    "org.apache.pekko"          %% "pekko-connectors-json-streaming" % pekkoVersion
  )

  val test = Seq(
    "org.mockito"             % "mockito-core"           % "5.23.0",
    "org.scalatest"          %% "scalatest"              % "3.2.20",
    "org.typelevel"          %% "cats-core"              % catsVersion,
    "org.scalatestplus.play" %% "scalatestplus-play"     % "7.0.2",
    "org.scalatestplus"      %% "mockito-5-12"           % "3.2.19.0",
    "org.scalacheck"         %% "scalacheck"             % "1.19.0",
    "org.typelevel"          %% "discipline-scalatest"   % "2.3.0",
    "com.vladsch.flexmark"    % "flexmark-all"           % "0.64.8",
    "uk.gov.hmrc"            %% "bootstrap-test-play-30" % boostrapPlayVersion
  ).map(_ % Test)
}
