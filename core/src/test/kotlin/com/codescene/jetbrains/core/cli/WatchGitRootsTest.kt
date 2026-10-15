package com.codescene.jetbrains.core.cli

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchGitRootsTest {
    @Test
    fun `union dedupes repository and mapping roots`() {
        assertEquals(
            listOf("/repo"),
            WatchGitRoots.union(listOf("/repo"), listOf("/repo"), false),
        )
    }

    @Test
    fun `vcs mapping root without git repository still produces scopes`() {
        val gitRoots = WatchGitRoots.union(emptyList(), listOf("/repo"), false)
        assertEquals(
            mapOf("/repo" to listOf(".")),
            WatchScopes.scopes(gitRoots, listOf("/repo"), false),
        )
    }
}
