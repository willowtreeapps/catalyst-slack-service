name := """catalyst-slack-service"""
organization := "org.catalyst.biascorrect"

version := sys.env.getOrElse("VERSION_NUMBER", "1.0-SNAPSHOT")

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.1"

libraryDependencies += guice
PlayKeys.devSettings := Seq("play.server.http.port" -> "4542")

import com.typesafe.sbt.packager.docker._

dockerBaseImage := "adoptopenjdk/openjdk12:x86_64-alpine-jre-12.33"
dockerCommands ++= Seq(
  Cmd("USER", "root"),
  ExecCmd("RUN", "apk", "add", "--no-cache", "bash"),
)