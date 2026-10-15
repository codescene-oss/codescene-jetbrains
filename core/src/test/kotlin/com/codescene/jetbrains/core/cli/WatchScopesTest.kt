package com.codescene.jetbrains.core.cli

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchScopesTest {
    @Test
    fun `content root equal to git root gives dot`() {
        assertEquals(mapOf("/repo" to listOf(".")), WatchScopes.scopes(listOf("/repo"), listOf("/repo"), false))
    }

    @Test
    fun `multiple content roots under one git root`() {
        assertEquals(
            mapOf("/repo" to listOf("a", "b/c")),
            WatchScopes.scopes(listOf("/repo"), listOf("/repo/b/c", "/repo/a"), false),
        )
    }

    @Test
    fun `nested content roots collapse`() {
        assertEquals(
            mapOf("/repo" to listOf("a")),
            WatchScopes.scopes(listOf("/repo"), listOf("/repo/a", "/repo/a/b"), false),
        )
        assertEquals(
            mapOf("/repo" to listOf(".")),
            WatchScopes.scopes(listOf("/repo"), listOf("/repo", "/repo/a"), false),
        )
    }

    @Test
    fun `content root above git root watches the whole repository`() {
        assertEquals(
            mapOf("/w/repo" to listOf(".")),
            WatchScopes.scopes(listOf("/w/repo"), listOf("/w"), false),
        )
    }

    @Test
    fun `nested git roots pick the inner root`() {
        assertEquals(
            mapOf("/p/sub" to listOf("x")),
            WatchScopes.scopes(listOf("/p", "/p/sub"), listOf("/p/sub/x"), false),
        )
    }

    @Test
    fun `unrelated content and git roots are omitted`() {
        assertEquals(emptyMap<String, List<String>>(), WatchScopes.scopes(listOf("/repo"), listOf("/elsewhere"), false))
        assertEquals(emptyMap<String, List<String>>(), WatchScopes.scopes(listOf("/orphan"), emptyList(), false))
    }

    @Test
    fun `sibling prefix does not match`() {
        assertEquals(emptyMap<String, List<String>>(), WatchScopes.scopes(listOf("/repo"), listOf("/repo2"), false))
    }

    @Test
    fun `Windows paths ignore case and separators`() {
        assertEquals(
            mapOf("C:/Repo" to listOf("src")),
            WatchScopes.scopes(listOf("C:/Repo"), listOf("c:\\repo\\src"), true),
        )
        assertEquals(emptyMap<String, List<String>>(), WatchScopes.scopes(listOf("/Repo"), listOf("/repo/src"), false))
    }

    @Test
    fun `collapse removes nested and duplicate paths`() {
        assertEquals(listOf("a", "b"), WatchScopes.collapse(listOf("b", "a/x", "a", "b"), false))
    }
}
