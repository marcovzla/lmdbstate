name := "states"

scalaVersion := "2.12.12"

libraryDependencies ++= Seq(
  "ai.lum" %% "common" % "0.1.5",
  "org.apache.lucene" % "lucene-core" % "6.6.6",
  "org.apache.lucene" % "lucene-join" % "6.6.6",
  "org.lmdbjava" % "lmdbjava" % "0.8.1",
)
