package com.codescene.jetbrains.core.util

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeSmellUtilsTest {
    @Test
    fun `readGitignore returns empty list when project path is null`() {
        val result = readGitignore(null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `readGitignore returns empty list when no file exists`() {
        val dir = Files.createTempDirectory("codescene-gitignore-missing")
        val result = readGitignore(dir.toString())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `readGitignore returns trimmed non empty lines`() {
        val dir = Files.createTempDirectory("codescene-gitignore")
        Files.writeString(
            dir.resolve(".gitignore"),
            """
            .kt
            
              .js  
            """.trimIndent(),
        )

        val result = readGitignore(dir.toString())
        assertEquals(listOf(".kt", ".js"), result)
    }

    @Test
    fun `readGitignore returns lines from simple two line gitignore`() {
        val dir = Files.createTempDirectory("codescene-gitignore-lines")
        Files.writeString(dir.resolve(".gitignore"), ".kt\n.env")

        val result = readGitignore(dir.toString())
        assertEquals(listOf(".kt", ".env"), result)
    }

    @Test
    fun `isSupportedLanguage returns true for known extension`() {
        assertTrue(isSupportedLanguage("kt"))
    }

    @Test
    fun `isSupportedLanguage returns false for unknown extension`() {
        assertFalse(isSupportedLanguage("unknown"))
    }

    @Test
    fun `formatCodeSmellMessage includes details when present`() {
        assertEquals("Complex Method (high nesting)", formatCodeSmellMessage("Complex Method", "high nesting"))
    }

    @Test
    fun `formatCodeSmellMessage omits parentheses when details are empty`() {
        assertEquals("Complex Method", formatCodeSmellMessage("Complex Method", ""))
    }

    @Test
    fun `isFileSupportedForAnalysis true for supported extension in project not gitignored`() {
        assertTrue(
            isFileSupportedForAnalysis(
                extension = "java",
                inProjectContent = true,
                excludeGitignoreFiles = true,
                ignoredByGitignore = false,
            ),
        )
    }

    @Test
    fun `isFileSupportedForAnalysis false for unsupported extension`() {
        assertFalse(
            isFileSupportedForAnalysis(
                extension = "unsupported_extension",
                inProjectContent = true,
                excludeGitignoreFiles = true,
                ignoredByGitignore = false,
            ),
        )
    }

    @Test
    fun `isFileSupportedForAnalysis false when gitignore excludes and setting enabled`() {
        assertFalse(
            isFileSupportedForAnalysis(
                extension = "java",
                inProjectContent = true,
                excludeGitignoreFiles = true,
                ignoredByGitignore = true,
            ),
        )
    }

    @Test
    fun `isFileSupportedForAnalysis false when file not in project content`() {
        assertFalse(
            isFileSupportedForAnalysis(
                extension = "java",
                inProjectContent = false,
                excludeGitignoreFiles = true,
                ignoredByGitignore = false,
            ),
        )
    }

    @Test
    fun `isFileSupportedForAnalysis true when gitignored but excludeGitignoreFiles false`() {
        assertTrue(
            isFileSupportedForAnalysis(
                extension = "java",
                inProjectContent = true,
                excludeGitignoreFiles = false,
                ignoredByGitignore = true,
            ),
        )
    }
}
