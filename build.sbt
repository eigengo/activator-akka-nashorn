import sbt._
import Keys._

name := "activator-akka-nashorn"

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies ++= {
  val akkaVersion  = "2.3.2"
  val sprayVersion = "1.3.1"
  val json4sVersion = "3.2.10"
  Seq(
    "com.typesafe.akka" %% "akka-actor"     % akkaVersion,
    "io.spray"           % "spray-can"      % sprayVersion,
    "io.spray"           % "spray-routing"  % sprayVersion,
    "org.json4s"        %% "json4s-native"  % json4sVersion,
    "org.json4s"        %% "json4s-jackson" % json4sVersion,
    "org.specs2"        %% "specs2"         % "2.3.12"       % "test",
    "io.spray"           % "spray-testkit"  % sprayVersion   % "test",
    "com.typesafe.akka" %% "akka-testkit"   % akkaVersion    % "test"
  )
}

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)

unmanagedResourceDirectories in Compile <++= baseDirectory {
  base => Seq(base / "src/main/angular")
}

crossPaths := false
