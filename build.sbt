lazy val baseName = "Configuration"

def baseNameL = baseName.toLowerCase

lazy val projectVersion   = "0.3.1"

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
      "com.jhlabs"    %  "filters"                  % "2.0.235-1",
      "de.sciss"      %% "fileutil"                 % "1.1.1",
      "de.sciss"      %% "swingplus"                % "0.2.0",
      "de.sciss"      %% "numbers"                  % "0.1.1",
      "de.sciss"      %% "kollflitz"                % "0.2.0",
      "de.sciss"      %% "processor"                % "0.4.0",
      "de.sciss"      %  "prefuse-core"             % "1.0.0"
    )
  )
)

lazy val sound = Project(
  id        = s"$baseNameL-sound",
  base      = file("sound"),
  settings  = commonSettings ++ Seq(
    name        := s"$baseName-sound",
    description := "Sound installation",
    libraryDependencies ++= Seq(
      "de.sciss"          %% "soundprocesses-views"     % "2.18.1",
      "de.sciss"          %% "lucredata-core"           % "2.3.1",
      "de.sciss"          %% "lucredata-views"          % "2.3.1",
      "de.sciss"          %% "scalacolliderswing-core"  % "1.25.0",
      "de.sciss"          %% "lucrestm-bdb"             % "2.1.1",
      "com.github.scopt"  %% "scopt"                    % "3.3.0"
    ),
    mainClass in assembly := Some("de.sciss.configuration.Configuration"),
    assemblyJarName in assembly := s"$baseName.jar",
    target in assembly := baseDirectory.value
  )
)
