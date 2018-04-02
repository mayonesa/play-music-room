name := "music-listening-room"

version := "0.1"

scalaVersion := "2.11.8"

lazy val root = (project in file(".")).enablePlugins(PlayScala, DebianPlugin)

maintainer in Linux := "John Jimenez <mayonesa.cosmica@gmail.com>"

packageSummary in Linux := "Social music-listening rooms"

packageDescription := "Get social. Share your music tastes. Get turned-on to new tunes"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}

sources in (Compile, doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false