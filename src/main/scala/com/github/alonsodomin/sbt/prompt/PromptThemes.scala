package com.github.alonsodomin.sbt.prompt

import promptlets._

trait PromptThemes extends Promptlets with PromptletSeparators {

  /**
   * A minimalistic theme that will show:
   *
   *  - the current Git branch (in green for a clean working directory, in yellow for a dirty one)
   *  - the current project, including the root project of a multi-project build
   *
   * This theme is the default theme
   */
  val DefaultTheme = PromptTheme(
    List(
      gitBranch(clean = fg(green), dirty = fg(yellow)).padLeft("[").padRight("] "),
      currentProject(fg(248)).padRight(": ")
    )
  )

  /**
   * An advanced theme that uses a combination of fancy colors and unicode separators
   * to achieve a very polished effect.
   *
   * This prompt will show the following information:
   *
   *  - a handy marker to indicate you are in SBT (instead of a normal bash/zsh shell)
   *  - the current Git branch (in green for a clean working directory, in yellow for a dirty one)
   *  - the current project, including the root project of a multi-project build
   *
   * Note:
   *  This theme uses some special icon-like characters (the separator character
   *  and the Git branch icon) that are not supported by most fonts. In order
   *  for this theme to render correctly, you will need to configure your terminal
   *  with a Powerline-patched font.
   *  See https://github.com/Lokaltog/powerline-fonts
   *
   * This theme is based on a customized version of the "agnoster" oh-my-zsh theme.
   * More information here:
   *  - https://gist.github.com/agnoster/3712874
   *  - https://github.com/robbyrussell/oh-my-zsh/blob/master/themes/agnoster.zsh-theme
   */
  val ScalapenosTheme = PromptTheme(
    List(
      text(" SBT ", fg(235).bg(26)),
      gitBranch(clean = fg(235).bg(34), dirty = fg(235).bg(214)).padLeft("  ").padRight(" "),
      currentProject(fg(250).bg(235)).pad(" "),
      text(" ", NoStyle)
    ),
    (previous, next) => StyledText("", fg(previous.style.background).bg(next.style.background))
  )

  /**
   * The Blueforest theme is a powerline prompt in the blueforest palette:
   * the sbt logo, the current project, the current Git branch with the status
   * of the repository, and the Scala version of the project, joined by
   * powerline arrows and closed off with a tip.
   *
   * The segments step through the blueforest blues, from the logo to the
   * Scala version, and the branch and the status of the repository share the
   * colours of the Git segment of Starship, whatever the state of the
   * working tree is.
   *
   * The colors are true-color (24-bit) escapes, so the palette is exact
   * instead of the nearest ANSI 256 approximation. Your terminal needs to
   * support true color for that to make a difference.
   *
   * The theme uses Nerd Font glyphs for the logo and the separators, so you
   * will need a Nerd Font (or a Powerline-patched font) to render it
   * properly.
   */
  val BlueforestTheme = PromptTheme(
    List(
      text(" \ue68d ", bg("#0099FF").fg("#0A101D")),
      currentProject(bg("#1b2b40").fg("#91CCFF")).pad(" "),
      gitBranch(clean = bg("#273e5c").fg("#ffcc33"), dirty = bg("#273e5c").fg("#ffcc33"))
        .padLeft(" \ue0a0 ")
        .padRight(" "),
      gitStatus(bg("#273e5c").fg("#ffcc33")).padRight(" "),
      currentScalaVersion(bg("#375780").fg("#FF8080")).padLeft(" \ue737 ").padRight(" "),
      text(" ", NoStyle) // Blank space for separation between the prompt and the cursor
    ),
    (previous, next) => StyledText("\ue0b0", fg(previous.style.background).bg(next.style.background))
  )
}

object PromptThemes extends PromptThemes
