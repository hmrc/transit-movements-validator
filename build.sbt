import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings

val appName = "transit-movements-validator"

val silencerVersion = "1.7.7"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    majorVersion := 0,
    scalaVersion := "2.13.8",
    PlayKeys.playDefaultPort := 9496,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions += "-Wconf:src=routes/.*:s",
    javaOptions ++= Seq(
      "-Djdk.xml.maxOccurLimit=100000"
    )
  )
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(CodeCoverageSettings.settings: _*)
  .settings(inThisBuild(buildSettings))

// Settings for the whole build
lazy val buildSettings = Def.settings(
  scalafmtOnCompile := true
)
