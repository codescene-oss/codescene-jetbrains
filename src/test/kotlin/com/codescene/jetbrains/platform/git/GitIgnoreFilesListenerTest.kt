package com.codescene.jetbrains.platform.git

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitIgnoreFilesListenerTest {
    private val listener =
        GitIgnoreFilesListener(
            project = io.mockk.mockk(relaxed = true),
            gitRootPath = "/repo",
            observer = null,
        )

    @Test
    fun `isGitIgnorePath matches root gitignore`() {
        assertTrue(listener.isGitIgnorePath("/repo/.gitignore"))
        assertTrue(listener.isGitIgnorePath("C:\\repo\\.gitignore"))
    }

    @Test
    fun `isGitIgnorePath matches nested gitignore`() {
        assertTrue(listener.isGitIgnorePath("/repo/src/.gitignore"))
    }

    @Test
    fun `isGitIgnorePath rejects non-gitignore paths`() {
        assertFalse(listener.isGitIgnorePath("/repo/.codescene/config.json"))
        assertFalse(listener.isGitIgnorePath("/repo/.git/info/exclude"))
        assertFalse(listener.isGitIgnorePath("/repo/src/Foo.kt"))
    }
}
