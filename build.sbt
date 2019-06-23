val appName         = "play2-memcached-" + playShortName
val appVersion      = "0.10.0"
val spymemcached    = "net.spy" % "spymemcached" % "2.12.3"
val h2databaseTest  = "com.h2database" % "h2" % "1.4.196" % Test

lazy val baseSettings = Seq(
  scalaVersion := "2.12.8",
  crossScalaVersions := Seq("2.11.12", "2.12.8", "2.13.0"),
  parallelExecution in Test := false,
  fork in Test := true, // can be removed with sbt 1.3.0, see https://github.com/sbt/sbt/issues/4609
  // Workaround until omnidoc gets published for Scala 2.13
  // http://central.maven.org/maven2/com/typesafe/play/play-omnidoc_2.13/
  PlayKeys.playOmnidoc := false
)

def playShortName: String = {
  val version = play.core.PlayVersion.current
  val majorMinor = version.split("""\.""").take(2).mkString("")
  s"play$majorMinor"
}

lazy val root = Project("root", base = file("."))
  .settings(baseSettings: _*)
  .settings(
    publishLocal := {},
    publish := {}
  ).aggregate(plugin, scalaSample, javaSample)

lazy val plugin = Project(appName, base = file("plugin"))
  .settings(baseSettings: _*)
  .settings(
    libraryDependencies += h2databaseTest,
    libraryDependencies += spymemcached,
    libraryDependencies += playCore % Provided,
    libraryDependencies += cacheApi % Provided,
    libraryDependencies += specs2 % Test,
    organization := "com.github.mumoshu",
    version := appVersion,
    //scalacOptions += "-deprecation",
    publishTo := {
      val v = version.value
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
      else                             Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <url>https://github.com/mumoshu/play2-memcached</url>
        <licenses>
          <license>
            <name>BSD-style</name>
            <url>http://www.opensource.org/licenses/bsd-license.php</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:mumoshu/play2-memcached.git</url>
          <connection>scm:git:git@github.com:mumoshu/play2-memcached.git</connection>
        </scm>
        <developers>
          <developer>
            <id>you</id>
            <name>KUOKA Yusuke</name>
            <url>https://github.com/mumoshu</url>
          </developer>
        </developers>
      ),
    unmanagedSourceDirectories in Compile := {
      (unmanagedSourceDirectories in Compile).value ++ Seq((sourceDirectory in Compile).value)
    },
    unmanagedSourceDirectories in Test := {
      (unmanagedSourceDirectories in Test).value ++ Seq((sourceDirectory in Test).value)
    }
  )

lazy val scalaSample = Project(
  "scala-sample",
  file("samples/scala")
).enablePlugins(PlayScala)
.settings(baseSettings: _*)
.settings(
  libraryDependencies += cacheApi,
  libraryDependencies += h2databaseTest,
  publishLocal := {},
  publish := {}
).dependsOn(plugin)

lazy val javaSample = Project(
  "java-sample",
  file("samples/java")
).enablePlugins(PlayJava)
.settings(baseSettings: _*)
.settings(
  libraryDependencies += cacheApi,
  libraryDependencies += h2databaseTest,
  publishLocal := {},
  publish := {}
).dependsOn(plugin)
