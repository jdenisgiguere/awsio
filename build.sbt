import Dependencies._

val sharedSettings =  Seq(
    organization := "com.strad",
    crossScalaVersions := Seq("2.11.11", "2.12.6"),
    scalacOptions += "-Ypartial-unification",
    version := "0.1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.1.0",
      "org.typelevel" %% "cats-effect" % "1.0.0-RC2",
      scalaTest % Test
    )
)

lazy val root = project
  .settings(
    moduleName := "aws-io-root",
    name := "awsio-io-root"
  )
  .aggregate(s3, sns, sqs, util)
  .dependsOn(s3, sns, sqs, util)

lazy val s3 = project
  .settings(
    libraryDependencies ++= Seq(
      "software.amazon.awssdk" % "s3" % "2.0.0-preview-10"
    )
  ).settings(sharedSettings)

lazy val sns = project
  .settings(
    libraryDependencies ++= Seq(
      "software.amazon.awssdk" % "sns" % "2.0.0-preview-10"
    )
  ).settings(sharedSettings)

lazy val sqs = project
  .settings(
    libraryDependencies ++= Seq(
      "software.amazon.awssdk" % "sqs" % "2.0.0-preview-10"
    )
  ).settings(sharedSettings)

lazy val util = project
  .settings(
    libraryDependencies ++= Seq(
      "io.monix" %% "monix" % "3.0.0-RC1",
    )
  )
  .settings(sharedSettings)

lazy val examples = project
  .dependsOn(s3, sns, sqs, util)