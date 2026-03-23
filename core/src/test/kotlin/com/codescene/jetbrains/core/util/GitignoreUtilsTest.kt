package com.codescene.jetbrains.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitignoreUtilsTest {
    @Test
    fun `returns true when extension is excluded`() {
        assertTrue(isExcludedByGitignore("kt", listOf(".kt", ".js")))
    }

    @Test
    fun `returns false when extension is not excluded`() {
        assertFalse(isExcludedByGitignore("py", listOf(".kt", ".js")))
    }

    @Test
    fun `returns false when extension is null`() {
        assertFalse(isExcludedByGitignore(null, listOf(".kt")))
    }
}
