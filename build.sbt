ThisBuild / version := "1.0.0"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "location-streaming"
  )

val http4sVersion = "0.23.17"
val http4sNettyVersion = "0.5.4"
val circeVersion = "0.14.3"
val log4catsVersion = "2.5.0"
val logbackVersion = "1.2.11"//newer versions based on slf4j-api-2.x not compatible with AWS SDK
val cirisVersion = "3.0.0"
val fs2AWSVersion = "5.1.0"
val nettyHTTP2CodecVersion = "4.1.85.Final"//version should be consistent with other netty packages coming from http4s
val javaxXmlVersion = "2.3.1"

resolvers += "confluent" at "https://packages.confluent.io/maven/"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.http4s" %% "http4s-netty-client" % http4sNettyVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "is.cir" %% "ciris" % cirisVersion,
  "is.cir" %% "ciris-refined" % cirisVersion,
  "io.laserdisc" %% "fs2-aws-kinesis" % fs2AWSVersion,
  "io.laserdisc" %% "fs2-aws-s3" % fs2AWSVersion,
  "io.laserdisc" %% "fs2-aws-dynamodb" % fs2AWSVersion,
  "io.netty" % "netty-codec-http2" % nettyHTTP2CodecVersion,
  "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "javax.xml.bind" % "jaxb-api" % javaxXmlVersion
)

dependencyOverrides += "io.circe" %% "circe-parser" % circeVersion
