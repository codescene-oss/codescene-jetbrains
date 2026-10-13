package com.codescene.jetbrains.core.cli

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspacePathsTest {
    @Test
    fun `normalizeRel converts backslashes`() {
        assertEquals("src/App.kt", WorkspacePaths.normalizeRel("src\\App.kt"))
    }

    @Test
    fun `normalizeRel strips a leading dot segment`() {
        assertEquals("src/App.kt", WorkspacePaths.normalizeRel("./src/App.kt"))
    }

    @Test
    fun `normalizeRel keeps an already normalized path`() {
        assertEquals("src/main/App.kt", WorkspacePaths.normalizeRel("src/main/App.kt"))
    }

    @Test
    fun `sameRoot ignores case, separators and trailing slash on Windows`() {
        assertTrue(WorkspacePaths.sameRoot("C:\\repo", "c:/repo/", windows = true))
    }

    @Test
    fun `sameRoot is case sensitive elsewhere`() {
        assertFalse(WorkspacePaths.sameRoot("/repo", "/Repo", windows = false))
    }

    @Test
    fun `sameRoot ignores a trailing slash elsewhere`() {
        assertTrue(WorkspacePaths.sameRoot("/repo/", "/repo", windows = false))
    }

    @Test
    fun `sameRoot keeps the filesystem root`() {
        assertTrue(WorkspacePaths.sameRoot("/", "/", windows = false))
        assertFalse(WorkspacePaths.sameRoot("/", "/repo", windows = false))
    }

    @Test
    fun `sameRoot distinguishes different repositories`() {
        assertFalse(WorkspacePaths.sameRoot("C:\\repo", "C:\\other", windows = true))
    }
}
