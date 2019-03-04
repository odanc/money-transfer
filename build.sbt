val Http4sVersion = "0.18.22"
val Specs2Version = "4.1.0"
val LogbackVersion = "1.2.3"
val FUUIDVersion = "0.1.2"

lazy val root = (project in file("."))
  .settings(
    organization := "org.odanc",
    name := "money-transfer",
    version := "0.0.1",
    scalaVersion := "2.12.8",
    libraryDependencies ++= Seq(
      "org.http4s"        %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"        %% "http4s-circe"        % Http4sVersion,
      "org.http4s"        %% "http4s-dsl"          % Http4sVersion,
      "org.typelevel"     %% "cats-effect"         % "1.2.0",
      "io.circe"          %% "circe-generic"       % "0.10.0",
      "io.chrisdavenport" %% "fuuid-http4s"        % FUUIDVersion,
      "io.chrisdavenport" %% "fuuid-circe"         % FUUIDVersion,
      "org.specs2"        %% "specs2-core"         % Specs2Version   % "test",
      "ch.qos.logback"    %  "logback-classic"     % LogbackVersion
    ),
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ypartial-unification"
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.6"),
    addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.2.4")
  )

trapExit := false