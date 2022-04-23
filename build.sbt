organization := "com.manenkov"
name := "assistant"
version := "0.0.1-SNAPSHOT"
crossScalaVersions := Seq("2.12.15", "2.13.6")

resolvers += Resolver.sonatypeRepo("snapshots")

val CatsVersion = "2.7.0"
val CirceVersion = "0.14.1"
val CirceGenericExVersion = "0.14.1"
val CirceConfigVersion = "0.8.0"
val DoobieVersion = "0.13.4"
val EnumeratumCirceVersion = "1.7.0"
val PostgreSQLVersion = "42.3.3"
val Http4sVersion = "0.21.28"
val KindProjectorVersion = "0.13.2"
val LogbackVersion = "1.2.11"
val Slf4jVersion = "1.7.32"
val ScalaCheckVersion = "1.15.4"
val ScalaTestVersion = "3.2.10"
val ScalaTestPlusVersion = "3.2.2.0"
val FlywayVersion = "8.5.4"
val SttpVersion = "3.5.1"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % CatsVersion,
  "io.circe" %% "circe-generic" % CirceVersion,
  "io.circe" %% "circe-literal" % CirceVersion,
  "io.circe" %% "circe-generic-extras" % CirceGenericExVersion,
  "io.circe" %% "circe-parser" % CirceVersion,
  "io.circe" %% "circe-config" % CirceConfigVersion,
  "org.tpolecat" %% "doobie-core" % DoobieVersion,
  "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
  "org.tpolecat" %% "doobie-scalatest" % DoobieVersion,
  "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
  "com.beachape" %% "enumeratum-circe" % EnumeratumCirceVersion,
  "org.postgresql" % "postgresql" % PostgreSQLVersion,
  "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
  "org.http4s" %% "http4s-circe" % Http4sVersion,
  "org.http4s" %% "http4s-dsl" % Http4sVersion,
  "ch.qos.logback" % "logback-classic" % LogbackVersion,
  "org.flywaydb" % "flyway-core" % FlywayVersion,
  "com.softwaremill.sttp.client3" %% "core" % SttpVersion,
  "com.softwaremill.sttp.client3" %% "circe" % SttpVersion,
  "org.http4s" %% "http4s-blaze-client" % Http4sVersion % Test,
  "org.scalacheck" %% "scalacheck" % ScalaCheckVersion % Test,
  "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
  "org.scalatestplus" %% "scalacheck-1-14" % ScalaTestPlusVersion % Test,
)

dependencyOverrides += "org.slf4j" % "slf4j-api" % Slf4jVersion

// Ad-hoc, need to be fixed
assembly / assemblyMergeStrategy := {
  case PathList("scala", "annotation", "nowarn.class") => MergeStrategy.discard
  case PathList("scala", "annotation", "nowarn$.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

addCompilerPlugin(
  ("org.typelevel" %% "kind-projector" % KindProjectorVersion).cross(CrossVersion.full),
)

enablePlugins(ScalafmtPlugin)