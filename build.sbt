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
    "org.scalanlp" % "nak" % "1.2.1",
    "nz.ac.waikato.cms.weka" % "weka-stable" % "3.8.0",
    "io.swave" %% "swave-core" % "0.5-M2"
)


fork in run := true
javaOptions ++= Seq("-Xms1024M", "-Xmx8G", "-XX:+UseConcMarkSweepGC")



