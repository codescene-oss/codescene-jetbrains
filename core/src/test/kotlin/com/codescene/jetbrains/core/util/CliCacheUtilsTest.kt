package com.codescene.jetbrains.core.util

import java.time.Duration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CliCacheUtilsTest {
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
