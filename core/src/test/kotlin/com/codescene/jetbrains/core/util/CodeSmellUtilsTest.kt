package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.testdoubles.StubGitService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeSmellUtilsTest {
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
        assertFileSupported(
            expected = true,
            extension = "java",
            inProjectContent = true,
            ignoredByGitignore = false,
        )
    }

    @Test
    fun `isFileSupportedForAnalysis false for unsupported extension`() {
        assertFileSupported(
            expected = false,
            extension = "unsupported_extension",
            inProjectContent = true,
            ignoredByGitignore = false,
        )
    }

    @Test
    fun `isFileSupportedForAnalysis false when gitignore excludes`() {
        assertFileSupported(
            expected = false,
            extension = "java",
            inProjectContent = true,
            ignoredByGitignore = true,
        )
    }

    @Test
    fun `isFileSupportedForAnalysis false when file not in project content`() {
        assertFileSupported(
            expected = false,
            extension = "java",
            inProjectContent = false,
            ignoredByGitignore = false,
        )
    }

    @Test
    fun `isFileSupportedForAnalysis asks git service for ignore status`() {
        val filePath = "repo/src/Main.kt"
        val result =
            isFileSupportedForAnalysis(
                extension = "kt",
                inProjectContent = true,
                filePath = filePath,
                gitService = StubGitService(ignoredByPath = mapOf(filePath to true)),
            )

        assertFalse(result)
    }

    @Test
    fun `isFileSupportedForAnalysis keeps supported file when git service says not ignored`() {
        val filePath = "repo/src/Main.kt"
        val result =
            isFileSupportedForAnalysis(
                extension = "kt",
                inProjectContent = true,
                filePath = filePath,
                gitService = StubGitService(ignoredByPath = mapOf(filePath to false)),
            )

        assertTrue(result)
    }

    @Test
    fun `linePairToOffsets resolves one based line numbers to offsets`() {
        val result =
            linePairToOffsets(
                startLineOneBased = 2,
                endLineOneBased = 4,
                lineStartOffset = { line -> line * 10 },
                lineEndOffset = { line -> (line * 10) + 9 },
            )

        assertEquals(10 to 39, result)
    }

    private fun assertFileSupported(
        expected: Boolean,
        extension: String?,
        inProjectContent: Boolean,
        ignoredByGitignore: Boolean,
    ) {
        val result =
            isFileSupportedForAnalysis(
                extension = extension,
                inProjectContent = inProjectContent,
                ignoredByGitignore = ignoredByGitignore,
            )

        assertEquals(expected, result)
    }
}
