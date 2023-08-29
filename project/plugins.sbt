ivyLoggingLevel := UpdateLogging.Full

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

val playVersion = scala.util.Properties.envOrElse("PLAY_VERSION", "2.5.19")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % playVersion)

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

