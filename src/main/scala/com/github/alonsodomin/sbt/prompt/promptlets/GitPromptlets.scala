package com.github.alonsodomin.sbt.prompt
package promptlets

import sbt._

import Keys._

/**
 * The promptlets that read the state of the Git repository of the project.
 *
 * The state is read with the porcelain format of version 2 of Git, the same
 * one Starship reads, so that the status of the repository is described the
 * same way in both of them.
 */
trait GitPromptlets extends Styles {
  import com.github.sbt.git.SbtGit._

  /**
   * The current Git branch, with a style for the clean and the dirty states
   * of the working tree.
   */
  def gitBranch(clean: Style = NoStyle, dirty: Style = NoStyle): Promptlet = gitPromptlet {
    case Some(git) => StyledText(git.branch, if (git.status.dirty) dirty else clean)
    case None      => StyledText.Empty
  }

  /**
   * The status of the repository, with the symbols Starship uses: one per
   * state, with the number of files or commits behind it.
   */
  def gitStatus(style: Style = NoStyle): Promptlet = gitPromptlet {
    case Some(git) => StyledText(gitStatusText(git.status), style)
    case None      => StyledText.Empty
  }

  /**
   * The status of the repository, with the symbols Starship uses.
   *
   * The symbols are the ones of the `git_status` module of Starship: `=` for
   * the conflicts, `$` for the stash, `✘` for the deleted files, `»` for the
   * renamed ones, `!` for the modified ones, `+` for the staged ones, and `?`
   * for the untracked ones. The commits that are not pushed yet come after
   * them: `⇡` for the ones ahead of the branch being tracked, `⇣` for the ones
   * behind it, and `⇕` for the ones that diverged from it.
   */
  def gitStatusText(status: GitStatus): String = {
    val files = List(
      "=" -> status.nrConflicted,
      "$" -> status.nrStashed,
      "✘" -> status.nrDeleted,
      "»" -> status.nrRenamed,
      "!" -> status.nrModified,
      "+" -> status.nrStaged,
      "?" -> status.nrUntracked
    ).collect { case (symbol, count) if count > 0 => s"$symbol$count" }

    val commits =
      if (status.nrAhead > 0 && status.nrBehind > 0) s"⇕⇡${status.nrAhead}⇣${status.nrBehind}"
      else if (status.nrAhead > 0) s"⇡${status.nrAhead}"
      else if (status.nrBehind > 0) s"⇣${status.nrBehind}"
      else ""

    (files :+ commits).mkString
  }

  /**
   * The more powerful form of `gitBranch`, which lets you use more specific
   * Git status information to produce some styled text. If the current
   * project is not a Git repository, the `Option[GitInfo]` parameter to the
   * `render` function will be `None`.
   */
  def gitPromptlet(render: Option[GitInfo] => StyledText): Promptlet = Promptlet(state => render(gitInfo(state)))

  case class GitInfo(branch: String, status: GitStatus)

  /**
   * The state of the repository.
   *
   * The files are counted twice when they are in both the index and the
   * working tree, the same way Starship counts them: a file that is staged
   * and modified afterwards is a staged file and a modified one.
   *
   * @param nrModified   the files modified in the working tree
   * @param nrUntracked  the files Git does not know about
   * @param nrStaged     the files changed in the index
   * @param nrDeleted    the files deleted, in the index or in the working tree
   * @param nrRenamed    the files renamed in the index
   * @param nrConflicted the files with a conflict in them
   * @param nrStashed    the entries of the stash
   * @param nrAhead      the commits ahead of the branch being tracked
   * @param nrBehind     the commits behind the branch being tracked
   */
  case class GitStatus(
      nrModified: Int,
      nrUntracked: Int,
      nrStaged: Int = 0,
      nrDeleted: Int = 0,
      nrRenamed: Int = 0,
      nrConflicted: Int = 0,
      nrStashed: Int = 0,
      nrAhead: Int = 0,
      nrBehind: Int = 0
  ) {

    /**
     * The number of changed files: the ones modified in the working tree and
     * the ones changed in the index.
     *
     * It is the number the themes that only care about whether the working
     * tree is clean need, and not `nrModified` alone, which only counts the
     * files of the working tree.
     */
    val nrChanged: Int = nrModified + nrStaged

    /**
     * Whether the working tree has changes in it.
     */
    val dirty: Boolean = nrChanged > 0 || nrUntracked > 0
  }

  // ==========================================================================
  // Implementation details
  // ==========================================================================

  private def gitInfo(state: State)(implicit extracted: Extracted = Project.extract(state)): Option[GitInfo] = {
    implicit val dir = extracted.get(baseDirectory)

    if (!isGitRepo(dir)) None
    else {
      Some(GitInfo(currentGitBranch(state), currentGitStatus(state)))
    }
  }

  private def currentGitBranch(state: State)(implicit dir: File, extracted: Extracted): String = {
    val reader           = extracted.get(GitKeys.gitReader)
    val branchFromReader = reader.withGit(_.branch)

    if (branchFromReader != null) branchFromReader
    else {
      val (_, runner) = extracted.runTask(GitKeys.gitRunner, state)

      runner("symbolic-ref", "--short", "HEAD")(dir, NoOpSbtLogger)
    }
  }

  private def currentGitStatus(state: State)(implicit dir: File, extracted: Extracted): GitStatus = {
    val (_, runner) = extracted.runTask(GitKeys.gitRunner, state)

    val status = parseGitStatus(
      runner("status", "--porcelain=2", "--branch", "--untracked-files=all")(dir, NoOpSbtLogger)
    )

    status.copy(nrStashed = parseGitStashes(runner("stash", "list")(dir, NoOpSbtLogger)))
  }

  /**
   * The state of the repository, read from the porcelain format of version 2
   * of Git.
   *
   * Every entry of the status is a line that starts with the kind of the
   * entry (`1` for an ordinary one, `2` for a renamed one, `u` for an
   * unmerged one, and `?` for an untracked file) and the state of the file in
   * the index and in the working tree. The headers of the branch come
   * first, and the one of the counts of the commits is the only one with the
   * number of commits ahead of and behind the branch being tracked.
   */
  private[prompt] def parseGitStatus(output: String): GitStatus = {
    output.split('\n').foldLeft(GitStatus(nrModified = 0, nrUntracked = 0)) {
      (status, line) =>
        if (line.startsWith("# branch.ab ")) {
          val counts = line.stripPrefix("# branch.ab ").split(' ').filter(_.nonEmpty)

          status.copy(
            nrAhead = counts.find(_.startsWith("+")).map(_.drop(1).toInt).getOrElse(0),
            nrBehind = counts.find(_.startsWith("-")).map(_.drop(1).toInt).getOrElse(0)
          )
        } else if (line.startsWith("? ")) {
          status.copy(nrUntracked = status.nrUntracked + 1)
        } else if (line.startsWith("u ")) {
          val (index, worktree) = gitStates(line)

          status.copy(
            nrConflicted = status.nrConflicted + 1,
            nrStaged = status.nrStaged + (if (index != '.') 1 else 0),
            nrModified = status.nrModified + (if (worktree != '.') 1 else 0)
          )
        } else if (line.startsWith("1 ") || line.startsWith("2 ")) {
          val (index, worktree) = gitStates(line)

          status.copy(
            nrStaged = status.nrStaged + (if (index != '.') 1 else 0),
            nrModified = status.nrModified + (if (worktree != '.') 1 else 0),
            nrDeleted = status.nrDeleted + (if (index == 'D' || worktree == 'D') 1 else 0),
            nrRenamed = status.nrRenamed + (if (index == 'R') 1 else 0)
          )
        } else {
          status
        }
    }
  }

  /**
   * The number of entries of the stash, read from the output of
   * `git stash list`.
   */
  private[prompt] def parseGitStashes(output: String): Int =
    output.split('\n').count(_.trim.nonEmpty)

  /**
   * The state of the file in the index and in the working tree, the two
   * characters that follow the kind of the entry.
   */
  private def gitStates(line: String): (Char, Char) = {
    val states = line.substring(2, 4)

    (states.charAt(0), states.charAt(1))
  }

  @scala.annotation.tailrec
  private def isGitRepo(dir: File): Boolean = {
    if (dir.listFiles().map(_.getName).contains(".git")) true
    else {
      val parent = dir.getParentFile
      if (parent == null) false
      else isGitRepo(parent)
    }
  }

  private object NoOpSbtLogger extends Logger {
    def trace(t: => Throwable): Unit                      = {}
    def success(message: => String): Unit                 = {}
    def log(level: Level.Value, message: => String): Unit = {}
  }
}

object GitPromptlets extends GitPromptlets
