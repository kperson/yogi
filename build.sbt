lazy val scalaTestVersion = "2.2.6"

lazy val commonSettings = Seq(
  organization := "com.kelt",
  version := "1.1.2",
  scalaVersion := "2.11.8",
  parallelExecution in Test := false,
  publishTo := Some(Resolver.file("file",  new File( "releases")))
)

lazy val client = (project in file("client")).
  settings(commonSettings: _*).
  settings(libraryDependencies ++= Seq(
    "net.databinder.dispatch" %% "dispatch-core"    % "0.12.0",
    "org.scalatest"           %% "scalatest"        %  scalaTestVersion % "test"
))

lazy val server = (project in file("server")).
  settings(commonSettings: _*).
  settings(libraryDependencies ++= Seq(
    "com.typesafe.akka"       %% "akka-http-core"   % "2.4.11",
    "org.scalatra"            %% "scalatra"         % "2.4.0",
    "commons-io"              %  "commons-io"       % "2.4",
    "com.netaporter"          %% "scala-uri"        % "0.4.14",
    "org.scalatest"           %% "scalatest"        %  scalaTestVersion % "test"

)).dependsOn(client, client % "test->compile")

lazy val demo = (project in file("demo")).
  settings(
      publishArtifact := false
  ).
  settings(commonSettings: _*).
  dependsOn(server)
