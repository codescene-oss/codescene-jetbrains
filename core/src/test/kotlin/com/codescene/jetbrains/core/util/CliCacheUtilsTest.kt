package com.codescene.jetbrains.core.util

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CliCacheUtilsTest {
    @Test
    fun `resolveCliCacheFileName uses repo relative path when available`() {
        val result = resolveCliCacheFileName("/repo/src/Main.kt", "src/Main.kt")

        assertEquals("./src/Main.kt", result)
    }

    @Test
    fun `resolveCliCacheFileName falls back to full file path`() {
        val result = resolveCliCacheFileName("/repo/src/Main.kt", null)

        assertEquals("/repo/src/Main.kt", result)
    }

    @Test
    fun `resolveBaselineCliCacheFileName prefixes commit when available`() {
        val result = resolveBaselineCliCacheFileName("/repo/src/Main.kt", "src/Main.kt", "abc123")

        assertEquals("abc123:./src/Main.kt", result)
    }

    @Test
    fun `resolveBaselineCliCacheFileName falls back to current file name without commit`() {
        val result = resolveBaselineCliCacheFileName("/repo/src/Main.kt", "src/Main.kt", null)

        assertEquals("./src/Main.kt", result)
    }

    @Test
    fun `isExpiredCliCacheEntry returns true when file age reaches max age`() {
        val result =
            isExpiredCliCacheEntry(
                lastModifiedMillis = 10_000L,
                nowMillis = 10_000L + Duration.ofDays(30).toMillis(),
                maxAge = Duration.ofDays(30),
            )

        assertTrue(result)
    }

    @Test
    fun `isExpiredCliCacheEntry returns false when file age is below max age`() {
        val result =
            isExpiredCliCacheEntry(
                lastModifiedMillis = 10_000L,
                nowMillis = 10_000L + Duration.ofDays(29).toMillis(),
                maxAge = Duration.ofDays(30),
            )

        assertFalse(result)
    }
}
