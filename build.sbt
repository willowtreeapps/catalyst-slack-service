name := """catalyst-slack-service"""
organization := "org.catalyst"

version := sys.env.getOrElse("VERSION_NUMBER", "1.0-SNAPSHOT")

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.1"

libraryDependencies += guice
libraryDependencies ++= Seq(
  javaWs
)
// https://mvnrepository.com/artifact/redis.clients/jedis
libraryDependencies += "redis.clients" % "jedis" % "3.2.0"

// https://mvnrepository.com/artifact/commons-codec/commons-codec
libraryDependencies += "commons-codec" % "commons-codec" % "1.13"

PlayKeys.devSettings := Seq("play.server.http.port" -> "4542")

import com.typesafe.sbt.packager.docker._

dockerPermissionStrategy := DockerPermissionStrategy.Run
dockerVersion := Some(DockerVersion(18, 9, 0, Some("ce")))
dockerRepository := sys.env.get("DOCKER_REPOSITORY")
dockerBaseImage := "adoptopenjdk/openjdk12:x86_64-alpine-jre-12.33"
dockerCommands ++= Seq(
  Cmd("USER", "root"),
  ExecCmd("RUN", "apk", "add", "--no-cache", "bash"),
)
