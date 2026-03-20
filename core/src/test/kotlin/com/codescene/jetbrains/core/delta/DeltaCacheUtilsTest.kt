package com.codescene.jetbrains.core.delta

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.testdoubles.InMemoryDeltaCacheService
import com.codescene.jetbrains.core.testdoubles.StubGitService
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class DeltaCacheUtilsTest {
    @Test
    fun `getCachedDelta returns miss when entry does not exist`() {
        val git = StubGitService(contentByPath = mapOf("a.kt" to "old"))
        val cache = InMemoryDeltaCacheService()

        val result = getCachedDelta("a.kt", "current", git, cache)
        assertEquals(false, result.first)
        assertNull(result.second)
    }

    @Test
    fun `getCachedDelta returns cached entry when key matches`() {
        val git = StubGitService(contentByPath = mapOf("a.kt" to "old"))
        val cache = InMemoryDeltaCacheService()
        val delta = mockk<Delta>(relaxed = true)
        cache.put(DeltaCacheEntry("a.kt", "old", "current", delta))

        val result = getCachedDelta("a.kt", "current", git, cache)
        assertEquals(true, result.first)
        assertSame(delta, result.second)
    }
}
