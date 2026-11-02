package com.github.alonsodomin.sbt.prompt
package promptlets

/**
 * The tests of the reading of the state of the repository.
 *
 * The states are the ones Git reports in the porcelain format of version 2,
 * the same format Starship reads.
 */
class GitPromptletsSuite extends munit.FunSuite {

  test("a clean repository has nothing in it") {
    val status = GitPromptlets.parseGitStatus(
      """# branch.oid 0f54910cb208fbb015590c6bc2348fdc102ac70e
        |# branch.head main
        |""".stripMargin
    )

    assertEquals(status.nrChanged, 0)
    assertEquals(status.dirty, false)
    assertEquals(GitPromptlets.gitStatusText(status), "")
  }

  test("a staged file is a changed file") {
    val status = GitPromptlets.parseGitStatus(
      """1 A. N... 000000 100644 100644 0000000000000000000000000000000000000000 2bdf67abb163a4ffb2d7f3f0880c9fe5068ce782 b.txt
        |""".stripMargin
    )

    assertEquals(status.nrStaged, 1)
    assertEquals(status.nrModified, 0)
    assertEquals(status.nrChanged, 1)
    assertEquals(status.dirty, true)
    assertEquals(GitPromptlets.gitStatusText(status), "+1")
  }

  test("a modified file is a changed file") {
    val status = GitPromptlets.parseGitStatus(
      """1 .M N... 100644 100644 100644 5626abf0f72e58d7a153368ba57db4c673c0e171 5626abf0f72e58d7a153368ba57db4c673c0e171 a.txt
        |""".stripMargin
    )

    assertEquals(status.nrStaged, 0)
    assertEquals(status.nrModified, 1)
    assertEquals(status.nrChanged, 1)
    assertEquals(status.dirty, true)
    assertEquals(GitPromptlets.gitStatusText(status), "!1")
  }

  test("a file that is staged and modified afterwards is in both of them") {
    val status = GitPromptlets.parseGitStatus(
      """1 MM N... 100644 100644 100644 5626abf0f72e58d7a153368ba57db4c673c0e171 5626abf0f72e58d7a153368ba57db4c673c0e171 a.txt
        |""".stripMargin
    )

    assertEquals(status.nrStaged, 1)
    assertEquals(status.nrModified, 1)
    assertEquals(status.nrChanged, 2)
    assertEquals(status.dirty, true)
    assertEquals(GitPromptlets.gitStatusText(status), "!1+1")
  }

  test("the untracked files are counted") {
    val status = GitPromptlets.parseGitStatus(
      """? c.txt
        |? d/e.txt
        |""".stripMargin
    )

    assertEquals(status.nrUntracked, 2)
    assertEquals(status.nrChanged, 0)
    assertEquals(status.dirty, true)
    assertEquals(GitPromptlets.gitStatusText(status), "?2")
  }

  test("a deleted file is a deleted file and a staged one") {
    val status = GitPromptlets.parseGitStatus(
      """1 D. N... 100644 000000 000000 5626abf0f72e58d7a153368ba57db4c673c0e171 0000000000000000000000000000000000000000 a.txt
        |""".stripMargin
    )

    assertEquals(status.nrDeleted, 1)
    assertEquals(status.nrStaged, 1)
    assertEquals(status.dirty, true)
    assertEquals(GitPromptlets.gitStatusText(status), "✘1+1")
  }

  test("a deleted file in the working tree is a deleted file and a modified one") {
    val status = GitPromptlets.parseGitStatus(
      """1 .D N... 100644 000000 000000 5626abf0f72e58d7a153368ba57db4c673c0e171 0000000000000000000000000000000000000000 a.txt
        |""".stripMargin
    )

    assertEquals(status.nrDeleted, 1)
    assertEquals(status.nrModified, 1)
    assertEquals(status.dirty, true)
  }

  test("a renamed file is a renamed file and a staged one") {
    val status = GitPromptlets.parseGitStatus(
      """2 R. N... 100644 100644 100644 5626abf0f72e58d7a153368ba57db4c673c0e171 5626abf0f72e58d7a153368ba57db4c673c0e171 R100 renamed.txt	a.txt
        |""".stripMargin
    )

    assertEquals(status.nrRenamed, 1)
    assertEquals(status.nrStaged, 1)
    assertEquals(status.dirty, true)
    assertEquals(GitPromptlets.gitStatusText(status), "»1+1")
  }

  test("a conflict is a conflict") {
    val status = GitPromptlets.parseGitStatus(
      """u UU N... 100644 100644 100644 100644 df967b96a579e45a18b8251732d16804b2e56a55 ba2906d0666cf726c7eaadd2cd3db615dedfdf3a e45c9c2666d44e0327c1f9c239a74c508336053e f.txt
        |""".stripMargin
    )

    assertEquals(status.nrConflicted, 1)
    assertEquals(status.nrChanged, 2)
    assertEquals(status.dirty, true)
    assertEquals(GitPromptlets.gitStatusText(status), "=1!1+1")
  }

  test("the commits that are not pushed yet are counted") {
    val ahead = GitPromptlets.parseGitStatus(
      """# branch.upstream origin/feature
        |# branch.ab +2 -0
        |""".stripMargin
    )

    assertEquals(ahead.nrAhead, 2)
    assertEquals(ahead.nrBehind, 0)
    assertEquals(GitPromptlets.gitStatusText(ahead), "⇡2")

    val behind = GitPromptlets.parseGitStatus(
      """# branch.upstream origin/feature
        |# branch.ab +0 -3
        |""".stripMargin
    )

    assertEquals(behind.nrBehind, 3)
    assertEquals(GitPromptlets.gitStatusText(behind), "⇣3")

    val diverged = GitPromptlets.parseGitStatus(
      """# branch.upstream origin/feature
        |# branch.ab +2 -3
        |""".stripMargin
    )

    assertEquals(diverged.nrAhead, 2)
    assertEquals(diverged.nrBehind, 3)
    assertEquals(GitPromptlets.gitStatusText(diverged), "⇕⇡2⇣3")
  }

  test("the entries of the stash are counted") {
    assertEquals(GitPromptlets.parseGitStashes(""), 0)
    assertEquals(
      GitPromptlets.parseGitStashes(
        """stash@{0}: WIP on main: 0f54910 On the main branch
          |stash@{1}: WIP on main: 4e5d882 On the main branch
          |""".stripMargin
      ),
      2
    )
  }

  /**
   * The status of the repository used to be read with the porcelain format
   * of version 1 of Git, where a repository was dirty when it had a file
   * that was not untracked in it. The themes that show the branch in a
   * different colour when the repository is dirty depend on that, so the two
   * ways of telling have to agree.
   */
  test("the repository is dirty in the same cases as it was before") {
    val repositories = List(
      (("", ""), false),
      (("M  a.txt", "1 M. N... 100644 100644 100644 5626abf0 5626abf0 a.txt"), true),
      ((" M a.txt", "1 .M N... 100644 100644 100644 5626abf0 5626abf0 a.txt"), true),
      (("MM a.txt", "1 MM N... 100644 100644 100644 5626abf0 5626abf0 a.txt"), true),
      (("A  a.txt", "1 A. N... 000000 100644 100644 0000000 5626abf0 a.txt"), true),
      ((" D a.txt", "1 .D N... 100644 000000 000000 5626abf0 0000000 a.txt"), true),
      (("D  a.txt", "1 D. N... 100644 000000 000000 5626abf0 0000000 a.txt"), true),
      (("R  a.txt -> b.txt", "2 R. N... 100644 100644 100644 5626abf0 5626abf0 R100 b.txt\ta.txt"), true),
      (("UU a.txt", "u UU N... 100644 100644 100644 100644 df967b9 ba2906d e45c9c2 a.txt"), true),
      (("?? a.txt", "? a.txt"), true),
      (("?? a.txt\n?? b.txt", "? a.txt\n? b.txt"), true)
    )

    repositories.foreach {
      case ((porcelainV1, porcelainV2), dirty) =>
        val lines = porcelainV1.split('\n').map(_.trim).filter(_.nonEmpty)

        // The old status counted the files that were not untracked and the
        // ones that were, and the repository was dirty when it had any of
        // them.
        val old = lines.exists(!_.startsWith("?")) || lines.exists(_.startsWith("?"))
        val now = GitPromptlets.parseGitStatus(porcelainV2)

        assertEquals(old, dirty, s"the repository of '$porcelainV1' was not dirty before")
        assertEquals(now.dirty, old, s"the repository of '$porcelainV1' is not dirty as it was")
    }
  }
}
