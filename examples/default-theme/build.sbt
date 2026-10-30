import com.github.alonsodomin.sbt.prompt.SbtPrompt.autoImport._

lazy val defaultTheme = (project in file(".")).settings(
  name                 := "default-theme",
  version              := "0.1",
  description          := "Without any configuration, sbt-prompt will use the default theme.",
  startYear            := Some(2014),
  homepage             := Some(url("https://github.com/alonsodomin/sbt-prompt")),
  organization         := "com.github.alonsodomin",
  organizationHomepage := Some(url("https://github.com/alonsodomin/sbt-prompt")),
  licenses             := Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  promptTheme          := DefaultTheme
)
