name := """catalyst-slack-service"""
organization := "org.catalyst"

version := sys.env.getOrElse("VERSION_NUMBER", "1.0-SNAPSHOT")

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.1"

libraryDependencies += guice
libraryDependencies ++= Seq(
  javaWs
)

// https://mvnrepository.com/artifact/commons-codec/commons-codec
libraryDependencies += "commons-codec" % "commons-codec" % "1.13"
// https://mvnrepository.com/artifact/redis.clients/jedis
libraryDependencies += "redis.clients" % "jedis" % "3.2.0"
// https://mvnrepository.com/artifact/org.mockito/mockito-core
libraryDependencies += "org.mockito" % "mockito-core" % "3.2.4" % Test
libraryDependencies += "org.springframework" % "spring-webmvc" % "5.2.1.RELEASE"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.1"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.10.1"
libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.4.1"
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.14.1" % "test"

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

PlayKeys.devSettings := Seq("play.server.http.port" -> "4543")

import com.typesafe.sbt.packager.docker._

dockerPermissionStrategy := DockerPermissionStrategy.Run
dockerVersion := Some(DockerVersion(18, 9, 0, Some("ce")))
dockerRepository := sys.env.get("DOCKER_REPOSITORY")
dockerBaseImage := "adoptopenjdk/openjdk12:x86_64-alpine-jre-12.33"
dockerCommands ++= Seq(
  Cmd("USER", "root"),
  ExecCmd("RUN", "apk", "add", "--no-cache", "bash"),
)
