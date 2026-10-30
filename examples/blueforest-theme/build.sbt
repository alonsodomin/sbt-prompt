import com.scalapenos.sbt.prompt.SbtPrompt.autoImport._

lazy val blueforestTheme = (project in file(".")).settings(
  name        := "blueforest-theme",
  version     := "0.1",
  description := """This project has been configured with the Blueforest theme, a powerline
                      prompt that uses true color and Nerd Font glyphs, so it needs
                      a terminal that supports both to render correctly.
                      See the README for more details.""",
  startYear            := Some(2014),
  homepage             := Some(url("https://github.com/agemooij/sbt-prompt")),
  organization         := "com.scalapenos",
  organizationHomepage := Some(url("http://scalapenos.com/")),
  licenses             := Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  promptTheme          := BlueforestTheme
)
