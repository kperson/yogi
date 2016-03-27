lazy val scalaTestVersion = "2.2.6"

lazy val commonSettings = Seq(
  organization := "com.kelt",
  version := "1.0",
  scalaVersion := "2.11.7",
  parallelExecution in Test := false
)

lazy val client = (project in file("client")).
  settings(commonSettings: _*).
  settings(libraryDependencies ++= Seq(
    "net.databinder.dispatch" %% "dispatch-core"    % "0.11.3",
    "org.scalatest"           %% "scalatest"        %  scalaTestVersion % "test"
))

lazy val server = (project in file("server")).
  settings(commonSettings: _*).
  settings(libraryDependencies ++= Seq(
    "com.typesafe.akka"       %% "akka-http-core"   % "2.4.2",
    "org.scalatra"            %% "scalatra"         % "2.4.0",
    "commons-io"              %  "commons-io"       % "2.4",
    "org.scalatest"           %% "scalatest"        %  scalaTestVersion % "test"

)).dependsOn(client % "test->compile")