package com.codescene.jetbrains.core.git

import org.junit.Assert.assertEquals
import org.junit.Test

class GitPathUtilsTest {
    @Test
    fun `pathComparisonKey normalizes backslashes to forward slashes`() {
        val windowsPath = "C:\\repo\\src\\File.kt"
        val unixPath = "C:/repo/src/File.kt"
        assertEquals(pathComparisonKey(windowsPath), pathComparisonKey(unixPath))
    }

    @Test
    fun `pathComparisonKey lowercases Windows drive letters`() {
        assertEquals("c:/repo/file.kt", pathComparisonKey("C:\\repo\\file.kt"))
        assertEquals("c:/repo/file.kt", pathComparisonKey("C:/repo/file.kt"))
        assertEquals("d:/foo/bar.txt", pathComparisonKey("D:\\foo\\bar.txt"))
    }

    @Test
    fun `pathComparisonKey preserves case for non-Windows paths`() {
        assertEquals("/Home/User/File.kt", pathComparisonKey("/Home/User/File.kt"))
        assertEquals("relative/Path/File.kt", pathComparisonKey("relative/Path/File.kt"))
    }

    @Test
    fun `pathCacheKey delegates to pathComparisonKey`() {
        val path = "C:\\test\\file.kt"
        assertEquals(pathComparisonKey(path), pathCacheKey(path))
    }

    @Test
    fun `pathForLog returns basename by default`() {
        val previous = System.getProperty(LOG_FULL_PATHS_PROPERTY)
        try {
            System.clearProperty(LOG_FULL_PATHS_PROPERTY)
            assertEquals("Main.kt", pathForLog("C:/Users/dev/project/src/Main.kt"))
        } finally {
            if (previous == null) {
                System.clearProperty(LOG_FULL_PATHS_PROPERTY)
            } else {
                System.setProperty(LOG_FULL_PATHS_PROPERTY, previous)
            }
        }
    }

    @Test
    fun `pathForLog returns full path when diagnostics property is true`() {
        val previous = System.getProperty(LOG_FULL_PATHS_PROPERTY)
        try {
            System.setProperty(LOG_FULL_PATHS_PROPERTY, "true")
            val fullPath = "C:/Users/dev/project/src/Main.kt"
            assertEquals(fullPath, pathForLog(fullPath))
        } finally {
            if (previous == null) {
                System.clearProperty(LOG_FULL_PATHS_PROPERTY)
            } else {
                System.setProperty(LOG_FULL_PATHS_PROPERTY, previous)
            }
        }
    }
}
