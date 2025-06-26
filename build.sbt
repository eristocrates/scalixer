ThisBuild / scalaVersion := "3.4.1"

lazy val root = (project in file("."))
  .settings(
    name := "scalixer",
    version := "0.1.0",
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "3.10.1",
      "co.fs2" %% "fs2-io" % "3.10.1",
      "org.gnieh" %% "fs2-data-xml" % "1.12.0"
    )
  )
