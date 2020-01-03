name := """catalyst-slack-service"""
organization := "org.catalyst.biascorrect"

version := sys.env.getOrElse("VERSION_NUMBER", "1.0-SNAPSHOT")

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.1"

libraryDependencies += guice
libraryDependencies ++= Seq(
  javaWs
)
// https://mvnrepository.com/artifact/org.mockito/mockito-core
libraryDependencies += "org.mockito" % "mockito-core" % "3.2.4" % Test

PlayKeys.devSettings := Seq("play.server.http.port" -> "4542")

import com.typesafe.sbt.packager.docker._

dockerPermissionStrategy := DockerPermissionStrategy.Run
dockerVersion := Some(DockerVersion(18, 9, 0, Some("ce")))
dockerBaseImage := "adoptopenjdk/openjdk12:x86_64-alpine-jre-12.33"
import com.typesafe.sbt.packager.docker.DockerPermissionStrategy
dockerRepository := sys.env.get("DOCKER_REPOSITORY")
dockerCommands ++= Seq(
  Cmd("USER", "root"),
  ExecCmd("RUN", "apk", "add", "--no-cache", "bash"),
)

