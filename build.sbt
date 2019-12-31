name := """catalyst-slack-service"""
organization := "org.catalyst.biascorrect"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.1"

libraryDependencies += guice
PlayKeys.devSettings := Seq("play.server.http.port" -> "4542")