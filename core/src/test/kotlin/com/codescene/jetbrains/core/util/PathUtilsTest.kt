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
}
