ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "scala_httpd",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.0"
  )
