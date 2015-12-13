name := "BetfairTrader"

val commonSettings = Seq(
  organization := "wjba",
  version := "1.0",
  scalaVersion := "2.11.7",
  scalacOptions := Seq(
    "-deprecation",
    "-unchecked",
    "-feature",
    "-Xlint",
    "-language:reflectiveCalls",
    "-Xmax-classfile-name", "128"
    )
)

val akkaV             = "2.3.6"
val sprayV            = "1.3.2"

lazy val serverResolvers = Seq(
  "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases",
  "spray repo" at "http://repo.spray.io/"
)

lazy val serverDependencies = Seq(
  "io.spray" %% "spray-can" % sprayV,
  "io.spray" %% "spray-caching" % sprayV,
  "io.spray" %% "spray-client" % sprayV,
  "io.spray" %% "spray-routing" % sprayV,
  "io.spray" %% "spray-testkit" % sprayV,
  "com.typesafe.akka" %% "akka-slf4j" % akkaV,
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.typesafe.akka" %% "akka-testkit" % akkaV,
  "com.typesafe.akka" %% "akka-remote" % akkaV,
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.github.nscala-time" %% "nscala-time" % "1.4.0",
  "com.typesafe.play" %% "play-json" % "2.4.3",
  "com.github.tomakehurst" % "wiremock" % "1.46" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "provided",
  "org.mockito" % "mockito-all" % "1.9.5" % "provided",
  "org.mockito" % "mockito-core" % "1.9.5" % "provided",
  "org.apache.httpcomponents" % "httpclient" % "4.3.6",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test",
  "org.scalacheck" %% "scalacheck" % "1.12.5" % "test"
)

lazy val webServerDependencies = Seq(
  "com.typesafe.akka" %% "akka-remote" % akkaV,
  "com.google.inject" % "guice" % "4.0",
  "javax.inject" % "javax.inject" % "1",
  "org.webjars" % "bootstrap" % "3.3.4",
  "org.webjars" % "angularjs" % "1.3.15",
  "org.webjars" % "angular-ui-bootstrap" % "0.14.3",
  "org.webjars" % "angular-ui-router" % "0.2.15",
  "org.webjars.npm" % "ui-select" % "0.13.2",
  "org.mockito" % "mockito-core" % "1.10.19" % "test"
)

lazy val server = project.in(file("server"))
  .settings(commonSettings: _*)
  .settings(
    resolvers ++= serverResolvers,
    libraryDependencies ++= serverDependencies
  )

lazy val webServer = project.in(file("webServer"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= webServerDependencies,
    pipelineStages := Seq(uglify, digest, gzip),
    pipelineStages in Assets := Seq(),
    pipelineStages := Seq(uglify, digest, gzip),
    DigestKeys.algorithms += "sha1",
    UglifyKeys.uglifyOps := { js =>
      Seq((js.sortBy(_._2), "concat.min.js"))
    },
    routesGenerator := InjectedRoutesGenerator
  )
  .enablePlugins(PlayScala)
  .dependsOn(server)

lazy val BetfairTrader = project.in(file("."))
  .aggregate(server, webServer)

