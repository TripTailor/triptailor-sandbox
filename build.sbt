name := """triptailor-sandbox"""
version := "1.0"
scalaVersion := "2.11.7"

val akkaV        = "2.4.1"
val akkaStreamsV = "2.0.1"
val nlpAnalysisV = "3.5.2"
val ammoniteV    = "0.5.2"
val scalaTestV   = "2.2.4"

val nlpAnalysisDependencies = Seq(
  "edu.stanford.nlp" % "stanford-corenlp" % nlpAnalysisV,
  "edu.stanford.nlp" % "stanford-corenlp" % nlpAnalysisV classifier "models"
)

val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamsV
)

val ammoniteRepl = Seq(
  "com.lihaoyi" % "ammonite-repl" % ammoniteV cross CrossVersion.full
)

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % scalaTestV % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaV % Test
)

libraryDependencies ++=
  nlpAnalysisDependencies ++ akkaDependencies ++ ammoniteRepl ++ testDependencies

initialCommands in console := """ammonite.repl.Main.run("")"""

// Clear console at the start of each run
triggeredMessage in ThisBuild := Watched.clearWhenTriggered

// Ctrl-C quits to console view
cancelable in Global := true
