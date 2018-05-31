import sbtcrossproject.{crossProject, CrossType}

lazy val shared = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val server = project
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

