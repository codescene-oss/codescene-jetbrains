package com.codescene.jetbrains.core.git

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommittedChangesCommandTest {
    @Test
    fun `committedChangesLogArgs walks first-parent history with merge diffs disabled`() {
        val args = committedChangesLogArgs("base123")

        assertTrue("Should limit history to the first parent", args.contains("--first-parent"))
        assertTrue("Should not emit paths introduced by merge commits", args.contains("--diff-merges=off"))
    }

    @Test
    fun `committedChangesLogArgs lists only paths of added, copied, modified and renamed files`() {
        val args = committedChangesLogArgs("base123")

        assertTrue("Should list file names", args.contains("--name-only"))
        assertTrue("Should suppress commit headers", args.contains("--pretty=format:"))
        assertTrue("Should skip deletions", args.contains("--diff-filter=ACMR"))
    }

    @Test
    fun `committedChangesLogArgs excludes the base commit from the revision range`() {
        val args = committedChangesLogArgs("base123")

        assertEquals("base123..HEAD", args.last())
    }
}
