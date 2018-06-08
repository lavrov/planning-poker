import sbtcrossproject.{crossProject, CrossType}

lazy val shared = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % versions.circe,
      "io.circe" %%% "circe-generic" % versions.circe,
      "io.circe" %%% "circe-parser" % versions.circe,
    ),
    addCompilerPlugin(
      "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
    )
  )
lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js


lazy val server = project
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-http"            % versions.akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-spray-json" % versions.akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-xml"        % versions.akkaHttpVersion,
        "com.typesafe.akka" %% "akka-stream"          % versions.akkaVersion,

        "com.typesafe.akka" %% "akka-http-testkit"    % versions.akkaHttpVersion % Test,
        "com.typesafe.akka" %% "akka-testkit"         % versions.akkaVersion     % Test,
        "com.typesafe.akka" %% "akka-stream-testkit"  % versions.akkaVersion     % Test,
      ),
      mappings in (Compile, packageDoc) := Seq()
    )
  .dependsOn(sharedJvm)
  .enablePlugins(JavaAppPackaging)

lazy val client = project
  .enablePlugins(ScalaJSBundlerPlugin, WorkbenchPlugin)
  .dependsOn(sharedJs)
  .settings(
    scalaVersion := "2.12.4",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++= Seq(
      "io.github.outwatch" %%% "outwatch" % versions.outwatch,
      "com.github.werk" %%% "router4s" % versions.router4s
    ),
    npmDependencies in Compile ++= Seq(
      "bootstrap" -> "4.1.0",
      "jquery" -> "1.9.1",
      "popper.js" -> "1.14.0"
    ),
    npmDevDependencies in Compile ++= Seq(
      "style-loader" -> "0.21.0",
      "css-loader" -> "0.28.11"
    ),
    scalaJSUseMainModuleInitializer := true,
    webpackBundlingMode in fastOptJS := BundlingMode.LibraryOnly(),
    workbenchStartMode := WorkbenchStartModes.Manual,
    workbenchDefaultRootObject := Some(("client/target/scala-2.12/classes/index.html", "client/target"))
  )

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % versions.scalatest % Test
  )
)

lazy val versions = new {
  def outwatch = "1.0.0-RC2"
  def circe = "0.9.3"
  def akkaHttpVersion = "10.1.1"
  def akkaVersion  = "2.5.12"
  def router4s = "0.1.0-SNAPSHOT"
  def scalatest = "3.0.5"
}

