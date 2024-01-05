import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.12"

val appName = "transit-movements-validator"

val silencerVersion = "1.7.7"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    PlayKeys.playDefaultPort := 9496,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions += "-Wconf:src=routes/.*:s",
    javaOptions ++= Seq(
      "-Djdk.xml.maxOccurLimit=100000"
    )
  )
  .settings(CodeCoverageSettings.settings: _*)
  .settings(inThisBuild(buildSettings))

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(
    libraryDependencies ++= AppDependencies.test,
    Test / fork := true,
    javaOptions ++= Seq(
      "-Djdk.xml.maxOccurLimit=10000",
    )
  )

// Settings for the whole build
lazy val buildSettings = Def.settings(
  scalafmtOnCompile := true
)
