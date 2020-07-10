
name := "transaction-stream-processor"
version := "1.0"
scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "spark-core",
  "spark-hive",
  "spark-streaming",
  "spark-sql",
  "spark-sql-kafka-0-10",
  "spark-streaming-kafka-0-10"
).map( "org.apache.spark" %% _ % "2.4.5" % "provided")

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

resolvers += Resolver.url("bintray-sbt-plugins", url("http://dl.bintray.com/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)
