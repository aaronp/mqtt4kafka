import java.nio.file.Path

import sbt.Keys.resolvers
import sbt.librarymanagement._
import sbtbuildinfo.BuildInfoPlugin.autoImport.buildInfoPackage
import eie.io._

val username = "aaronp"
val repo = "mqtt4kafka"
val org = "com.github.aaronp"

organization := org
name := repo

enablePlugins(BuildInfoPlugin)

val scalacSettings = List(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-encoding",
  "utf-8", // Specify character encoding used by source files.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-language:reflectiveCalls", // Allow reflective calls
  "-language:higherKinds", // Allow higher-kinded types
  "-language:implicitConversions", // Allow definition of implicit functions called views
  "-unchecked",
  "-language:reflectiveCalls", // Allow reflective calls
  "-language:higherKinds", // Allow higher-kinded types
  "-language:implicitConversions", // Allow definition of implicit functions called views
  //"-Xlog-implicits",
  "-Xfuture" // Turn on future language features.
)

// tests start/stop e.g. kafka containers
parallelExecution in ThisBuild := false

Keys.test in (assembly in ThisBuild) := {}


val testDependencies = List(
  "junit" % "junit" % "4.12" % "test",
  "org.scalatest" %% "scalatest" % "3.0.7" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.1" % "test",
  "org.pegdown" % "pegdown" % "1.6.0" % "test",
)

val coreDependencies = List(
  "com.github.aaronp" %% "eie" % "0.0.5",
  "com.github.aaronp" %% "args4c" % "0.4.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe" % "config" % "1.3.3"
)


val commonSettings: Seq[Def.Setting[_]] = Seq(
  organization := org,
  scalaVersion := "2.12.8",
  autoAPIMappings := true,
  exportJars := false,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-XX:MaxMetaspaceSize=1g"),
  scalacOptions ++= scalacSettings,
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := s"${repo}.build",
  libraryDependencies ++= testDependencies,
  libraryDependencies ++= coreDependencies,
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := "mqtt4kafka.build",
  assemblyMergeStrategy in assembly := {
    case str if str.contains("application.conf") => MergeStrategy.discard
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  // see http://www.scalatest.org/user_guide/using_scalatest_with_sbt
  (testOptions in Test) += (Tests
    .Argument(TestFrameworks.ScalaTest, "-h", s"target/scalatest-reports-${name.value}", "-oN"))
)

lazy val root = (project in file("."))
  .aggregate(mqtt)
  .aggregate(test)


lazy val docker = taskKey[Unit]("Packages the app in a docker file").withRank(KeyRanks.APlusTask)

docker := {
  val mqttAssembly = (assembly in(mqtt, Compile)).value
  val deployResourceDir: Path = (resourceDirectory in(mqtt, Compile)).value.getParentFile.asPath.resolve("docker")

  val dockerDir = {
    val dir = baseDirectory.value / "target" / "docker"
    dir.toPath.mkDirs()
  }

  Build.docker(deployResourceDir, mqttAssembly.asPath, dockerDir, sLog.value)
}

lazy val mqtt = project
  .in(file("mqtt"))
  .settings(name := "mqtt", coverageMinimum := 90, coverageFailOnMinimum := true)
  .settings(commonSettings: _*)
  .settings(Keys.test in assembly := {})
  .settings(libraryDependencies ++= List(
    "com.lightbend.akka" %% "akka-stream-alpakka-mqtt-streaming" % "1.0-M3" % "test",
    "com.typesafe.akka" %% "akka-stream-kafka" % "1.0.1" % "test"
  ))
  .settings(libraryDependencies ++= List(
    "org.apache.kafka" % "kafka-clients" % "2.2.0",
    "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.2.1"
  ))


lazy val test = project
  .in(file("test"))
  .settings(name := "test")
  .settings(commonSettings: _*)
  .dependsOn(mqtt % "test->test")

pomIncludeRepository := (_ => false)

// To sync with Maven central, you need to supply the following information:
pomExtra in Global := {
  <url></url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>Aaron</id>
        <name>Aaron Pritzlaff</name>
        <url>http://github.com/aaronp</url>
      </developer>
    </developers>
}

