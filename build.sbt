name := "httplogmonitoring"

version := "1.0"

scalaVersion := "2.11.7"

mainClass in Global := Some("httplogmonitoring.Main")

assemblyJarName in assembly := "httplogmonitoring.jar"

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.5",
  "com.typesafe" % "config" % "1.3.0",
  "org.specs2" %% "specs2" % "2.3.13" % "test"
)
