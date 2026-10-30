package com.github.alonsodomin.sbt.prompt

/**
 * Represents an ANSI color escape code for either a foreground
 * color or a background color.
 */
sealed trait Color {
  val bg: String
  val fg: String
}

/**
 * Implements Color using the ANSI 256 color space.
 *
 * For some good references, see:
 *
 * - Ansi escape code reference: http://misc.flogisoft.com/bash/tip_colors_and_formatting
 * - Graphical overview of the Ansi 256 color codes: http://www.calmar.ws/vim/color-output.png
 */
private case class Ansi256Color(code: Int) extends Color {
  require((code >= 0 && code <= 255))

  val bg = s"\u001b[48;5;${code}m"
  val fg = s"\u001b[38;5;${code}m"
}

/**
 * Implements Color using the 24-bit true color space.
 *
 * Terminals that support *truecolor* (or *direct color*) can render any
 * color exactly, without having to approximate it with one of the 256
 * colors of the ANSI 256 color space.
 */
private case class RgbColor(red: Int, green: Int, blue: Int) extends Color {
  require((red >= 0 && red <= 255))
  require((green >= 0 && green <= 255))
  require((blue >= 0 && blue <= 255))

  val bg = s"\u001b[48;2;${red};${green};${blue}m"
  val fg = s"\u001b[38;2;${red};${green};${blue}m"
}

/**
 * Simple factory to create instances of Color based on their ANSI 256 color
 * space code. The number must be between 0 and 255 (inclusive).
 */
object Color {
  def apply(code: Int): Color = Ansi256Color(code)

  /**
   * Creates a Color in the 24-bit true color space. Each of the components
   * must be between 0 and 255 (inclusive).
   */
  def rgb(red: Int, green: Int, blue: Int): Color = RgbColor(red, green, blue)

  /**
   * Creates a Color in the 24-bit true color space from a hexadecimal color
   * code like "#0099FF". The leading "#" is optional.
   */
  def hex(code: String): Color = {
    val digits = code.stripPrefix("#")
    require(digits.length == 6, s"Invalid hexadecimal color code: $code")
    rgb(
      Integer.parseInt(digits.substring(0, 2), 16),
      Integer.parseInt(digits.substring(2, 4), 16),
      Integer.parseInt(digits.substring(4, 6), 16)
    )
  }
}

/**
 * Some named color aliases in the ANSI 16/256 color spaces.
 */
trait Colors {

  /** Implementation of Color using the ANSI "default" color codes. */
  case object NoColor extends Color {
    val bg = "\u001b[49m"
    val fg = "\u001b[39m"
  }

  val red     = Color(9)
  val green   = Color(10)
  val yellow  = Color(11)
  val blue    = Color(12)
  val magenta = Color(13)
  val cyan    = Color(14)
  val white   = Color(15)
}

object Colors extends Colors
