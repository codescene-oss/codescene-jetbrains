package com.codescene.jetbrains.core.cli

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun `rootKey normalizes slashes and trailing slash on Windows`() {
        assertEquals("c:/repo", WorkspacePaths.rootKey("C:\\Repo\\", windows = true))
    }

    @Test
    fun `rootKey keeps case elsewhere`() {
        assertEquals("/Repo", WorkspacePaths.rootKey("/Repo/", windows = false))
    }

    @Test
    fun `relativeTo returns dot for the root itself`() {
        assertEquals(".", WorkspacePaths.relativeTo("/repo", "/repo", windows = false))
    }

    @Test
    fun `relativeTo returns the path under the root`() {
        assertEquals("src/A.kt", WorkspacePaths.relativeTo("/repo", "/repo/src/A.kt", windows = false))
    }

    @Test
    fun `relativeTo rejects sibling prefixes`() {
        assertNull(WorkspacePaths.relativeTo("/repo", "/repo2/A.kt", windows = false))
    }

    @Test
    fun `relativeTo ignores case on Windows`() {
        assertEquals("Src", WorkspacePaths.relativeTo("C:\\Repo", "c:/repo/Src", windows = true))
    }
}
