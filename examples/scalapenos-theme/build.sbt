import com.github.alonsodomin.sbt.prompt.SbtPrompt.autoImport._

lazy val scalapenosTheme = (project in file(".")).settings(
  name        := "scalapenos-theme",
  version     := "0.1",
  description := """This project has been configured with the fancy Scalapenos theme, which
                      needs special font support to render correctly.
                      See the README for more details.""",
  startYear            := Some(2014),
  homepage             := Some(url("https://github.com/alonsodomin/sbt-prompt")),
  organization         := "com.github.alonsodomin",
  organizationHomepage := Some(url("https://github.com/alonsodomin/sbt-prompt")),
  licenses             := Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  promptTheme          := ScalapenosTheme
)
