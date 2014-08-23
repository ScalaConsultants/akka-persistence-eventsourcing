name := "akka-persistence-event-sourcing"

version := "1.0.0"

organization := "io.scalac"

scalaVersion := "2.11.2"

resolvers ++= Seq(
  "snapshots"           at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases"            at "http://oss.sonatype.org/content/repositories/releases",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "spray repo"          at "http://repo.spray.io"
)

seq(Revolver.settings: _*)

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

javaOptions := Seq("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000")

parallelExecution in Test := false

libraryDependencies ++= {
  val sprayVersion = "1.3.1"
  val akkaVersion = "2.3.5"
  Seq(
    "org.slf4j"               %   "slf4j-api"       % "1.7.7",
    "ch.qos.logback"          %   "logback-core"    % "1.1.2",
    "ch.qos.logback"          %   "logback-classic" % "1.1.2",
    "io.spray"                %%  "spray-can"       % sprayVersion,
    "io.spray"                %%  "spray-routing"   % sprayVersion,
    "io.spray"                %%  "spray-testkit"   % sprayVersion,
    "io.spray"                %%  "spray-httpx"     % sprayVersion,
    "io.spray"                %%  "spray-client"    % sprayVersion,
    "org.json4s"              %%  "json4s-native"   % "3.2.10",
    "joda-time"               %   "joda-time"       % "2.4",
    "org.joda"                %   "joda-convert"    % "1.7",
    "com.typesafe.akka"       %%  "akka-actor"      % akkaVersion,
    "com.typesafe.akka"       %%  "akka-slf4j"      % akkaVersion,
    "com.typesafe.akka"       %%  "akka-testkit"    % akkaVersion % "test",
    "com.typesafe.akka"       %%  "akka-persistence-experimental" % akkaVersion,
    "org.scalatest"           %%  "scalatest"       % "2.2.1" % "test",
    "io.spray"                %%  "spray-testkit"   % "1.3.1" % "test",
    "com.github.t3hnar"       %%  "scala-bcrypt"    % "2.4"
  )
}
