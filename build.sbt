ThisBuild / version := "1.0.0"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "ztm-fetcher"
  )

val http4sVersion = "0.23.17"
val http4sNettyVersion = "0.5.4"
val circeVersion = "0.14.3"
val log4catsVersion = "2.5.0"
val logbackVersion = "1.4.5"
val cirisVersion = "3.0.0"
val fs2AWSVersion = "5.1.0"

resolvers += "confluent" at "https://packages.confluent.io/maven/"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.http4s" %% "http4s-netty-client" % http4sNettyVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
  "is.cir" %% "ciris" % cirisVersion,
  "is.cir" %% "ciris-refined" % cirisVersion,
  "io.laserdisc" %% "fs2-aws-kinesis" % fs2AWSVersion,
  "io.laserdisc" %% "fs2-aws-s3" % fs2AWSVersion,
  "io.laserdisc" %% "fs2-aws-dynamodb" % fs2AWSVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime
)

dependencyOverrides += "io.circe" %% "circe-parser" % circeVersion
