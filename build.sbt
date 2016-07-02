name := "poc_analyzer"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % Test,
    "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.4.7",
    "com.typesafe.akka" % "akka-stream_2.11" % "2.4.7",
    "com.typesafe.akka" % "akka-testkit_2.11" % "2.4.7",
    "com.typesafe.akka" % "akka-actor_2.11" % "2.4.7",
    "com.typesafe.akka" % "akka-http-spray-json-experimental_2.11" % "2.4.7",
    "nz.ac.waikato.cms.weka" % "weka-stable" % "3.8.0"
)


fork in run := true
javaOptions ++= Seq("-Xms1024M", "-Xmx8G", "-XX:+UseConcMarkSweepGC")



