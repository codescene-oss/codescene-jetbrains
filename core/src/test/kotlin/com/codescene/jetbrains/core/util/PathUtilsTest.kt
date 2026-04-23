package com.codescene.jetbrains.core.util

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class PathUtilsTest {
    @Test
    fun `getRelativePath returns relative path between base and file`() {
        val result = getRelativePath("/home/user/project", "/home/user/project/src/Main.kt")
        assertEquals("src${File.separator}Main.kt", result)
    }

    @Test
    fun `getRelativePath returns empty when paths are the same`() {
        val result = getRelativePath("/home/user/project", "/home/user/project")
        assertEquals("", result)
    }

    @Test
    fun `getRelativePath handles parent traversal`() {
        val result = getRelativePath("/home/user/project/src", "/home/user/project/test/Main.kt")
        assertEquals("..${File.separator}test${File.separator}Main.kt", result)
    }

    @Test
    fun `extractExtension returns extension from filename`() {
        assertEquals("kt", extractExtension("Main.kt"))
    }

    @Test
    fun `extractExtension returns null for filename without extension`() {
        assertEquals(null, extractExtension("Makefile"))
    }

    @Test
    fun `extractExtension returns last segment for multiple dots`() {
        assertEquals("gz", extractExtension("archive.tar.gz"))
    }

    @Test
    fun `pathsAfterRename joins parent with old and new names`() {
        val parent = "${File.separator}repo${File.separator}src"
        val (oldPath, newPath) = pathsAfterRename(parent, "Old.kt", "New.kt")
        assertEquals(File(parent, "Old.kt").path, oldPath)
        assertEquals(File(parent, "New.kt").path, newPath)
    }
}
