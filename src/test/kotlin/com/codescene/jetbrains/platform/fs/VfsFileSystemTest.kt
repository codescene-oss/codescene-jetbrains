package com.codescene.jetbrains.platform.fs

import org.junit.Assert.assertEquals
import org.junit.Test

class VfsFileSystemTest {
    private val fs = VfsFileSystem()

    @Test
    fun `getRelativePath returns relative path between base and file`() {
        val result = fs.getRelativePath("/home/user/project", "/home/user/project/src/Main.kt")

        assertEquals("src${java.io.File.separator}Main.kt", result)
    }

    @Test
    fun `getRelativePath returns empty when paths are the same`() {
        val result = fs.getRelativePath("/home/user/project", "/home/user/project")

        assertEquals("", result)
    }

    @Test
    fun `getRelativePath handles parent traversal`() {
        val result = fs.getRelativePath("/home/user/project/src", "/home/user/project/test/Main.kt")

        assertEquals("..${java.io.File.separator}test${java.io.File.separator}Main.kt", result)
    }
}
