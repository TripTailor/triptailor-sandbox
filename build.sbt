name         := "triptailor-sandbox"
version      := "1.0"
scalaVersion := "2.11.8"

val jodaConvertV = "1.8.1"
val akkaV        = "2.4.14"
val nlpAnalysisV = "3.6.0"
val ammoniteV    = "0.7.8"
val scalaTestV   = "3.0.1"

val utilityDependencies = Seq(
  "org.joda" % "joda-convert" % jodaConvertV
)

val nlpAnalysisDependencies = Seq(
  "edu.stanford.nlp" % "stanford-corenlp" % nlpAnalysisV,
  "edu.stanford.nlp" % "stanford-corenlp" % nlpAnalysisV classifier "models"
)

val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.typesafe.akka" %% "akka-stream" % akkaV
)

val ammoniteRepl = Seq(
  "com.lihaoyi" % "ammonite" % ammoniteV cross CrossVersion.full
)

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % scalaTestV % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaV % Test
)

libraryDependencies ++=
  utilityDependencies ++ nlpAnalysisDependencies ++ akkaDependencies ++ ammoniteRepl ++ testDependencies

initialCommands in console := """ammonite.Main().run()"""

// Compile options
scalacOptions in Compile := Seq("-Xlint", "-deprecation", "-feature", "-unchecked", "-encoding", "utf8", "-Ywarn-dead-code")

// Clear console at the start of each run
triggeredMessage in ThisBuild := Watched.clearWhenTriggered

// Ctrl-C quits to console view
cancelable in Global := true
