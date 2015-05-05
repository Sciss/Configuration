lazy val baseName = "Configuration"

def baseNameL = baseName.toLowerCase

lazy val projectVersion   = "0.1.0-SNAPSHOT"

lazy val commonSettings = Project.defaultSettings ++ Seq(
  version            := projectVersion,
  organization       := "de.sciss",
  scalaVersion       := "2.11.6",
  homepage           := Some(url("https://github.com/Sciss/" + baseName)),
  licenses           := Seq("GPL v3+" -> url("http://www.gnu.org/licenses/gpl-3.0.txt")),
  scalacOptions     ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture")
)

lazy val video = Project(
  id        = s"$baseNameL-video",
  base      = file("video"),
  settings  = commonSettings ++ Seq(
    name        := s"$baseName-video",
    description := "Scripts for video processing",
    libraryDependencies ++= Seq(
      "com.jhlabs" %  "filters"   % "2.0.235-1",
      "de.sciss"   %% "fileutil"  % "1.1.1",
      "de.sciss"   %% "swingplus" % "0.2.0",
      "de.sciss"   %% "numbers"   % "0.1.1"
    )
  )
)
