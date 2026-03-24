package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GitReflogUtilsTest {
    @Test
    fun `parseBranchCreationCommitFromReflog returns first token from newest matching line`() {
        val lines =
            listOf(
                "abc123 HEAD@{0}: commit: msg",
                "def456 HEAD@{1}: Branch: created from main",
            )
        assertEquals("def456", parseBranchCreationCommitFromReflog(lines))
    }

    @Test
    fun `parseBranchCreationCommitFromReflog is case insensitive on marker`() {
        val lines = listOf("deadbeef HEAD@{0}: Created From origin/develop")
        assertEquals("deadbeef", parseBranchCreationCommitFromReflog(lines))
    }

    @Test
    fun `parseBranchCreationCommitFromReflog returns null when no match`() {
        assertNull(parseBranchCreationCommitFromReflog(listOf("only other content")))
    }

    @Test
    fun `parseBranchCreationCommitFromReflog returns null for empty list`() {
        assertNull(parseBranchCreationCommitFromReflog(emptyList()))
    }
}
