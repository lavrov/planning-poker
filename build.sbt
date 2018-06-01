import sbtcrossproject.{crossProject, CrossType}

lazy val shared = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val akkaHttpVersion = "10.1.1"
lazy val akkaVersion    = "2.5.12"

lazy val server = project
    .settings(
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-stream"          % akkaVersion,

        "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
        "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
        "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
        "org.scalatest"     %% "scalatest"            % "3.0.1"         % Test
      )
    )
  .dependsOn(sharedJvm)

lazy val client = project
  .enablePlugins(ScalaJSBundlerPlugin, WorkbenchPlugin)
  .dependsOn(sharedJs)
  .settings(
    scalaVersion := "2.12.4",
    libraryDependencies += "io.github.outwatch" %%% "outwatch" % "1.0.0-RC2",
    scalaJSUseMainModuleInitializer := true,
    webpackBundlingMode := BundlingMode.LibraryOnly(),
    workbenchStartMode := WorkbenchStartModes.Manual,
    workbenchDefaultRootObject := Some(("client/target/scala-2.12/classes/index.html", "client/target"))
  )

