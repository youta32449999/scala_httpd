ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val akkaVersion = "$akka_version$"

lazy val root = (project in file("."))
  .settings(
    name := "scala_httpd",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "org.scalatest" %% "scalatest" % "3.2.0"
    )
  )
