package com.codescene.jetbrains.core.review

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.TestLogger
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.codec.digest.DigestUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AceRefactorableFunctionsCacheServiceTest {
    private lateinit var cache: AceRefactorableFunctionsCacheService

    @Before
    fun setUp() {
        cache = AceRefactorableFunctionsCacheService(TestLogger)
    }

    private fun createFn(name: String = "fn"): FnToRefactor {
        val fn = mockk<FnToRefactor>(relaxed = true)
        every { fn.name } returns name
        return fn
    }

    @Test
    fun `get returns empty list when no entry exists`() {
        val result = cache.get("a.kt", "content")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `put and get returns cached result for matching content`() {
        val fns = listOf(createFn("fn1"))
        cache.put("a.kt", "content", fns)
        val result = cache.get("a.kt", "content")
        assertEquals(1, result.size)
        assertEquals("fn1", result[0].name)
    }

    @Test
    fun `get returns empty list when content does not match`() {
        cache.put("a.kt", "content", listOf(createFn()))
        val result = cache.get("a.kt", "different content")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `get returns empty list for wrong file path`() {
        cache.put("a.kt", "content", listOf(createFn()))
        val result = cache.get("b.kt", "content")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `get accepts sha256 hex as content`() {
        val content = "some code"
        val sha = DigestUtils.sha256Hex(content)
        cache.put("a.kt", content, listOf(createFn("fn1")))
        val result = cache.get("a.kt", sha)
        assertEquals(1, result.size)
    }

    @Test
    fun `put overwrites previous entry for same file`() {
        cache.put("a.kt", "v1", listOf(createFn("old")))
        cache.put("a.kt", "v2", listOf(createFn("new")))
        assertTrue(cache.get("a.kt", "v1").isEmpty())
        assertEquals("new", cache.get("a.kt", "v2")[0].name)
    }

    @Test
    fun `invalidate removes entry`() {
        cache.put("a.kt", "content", listOf(createFn()))
        cache.invalidate("a.kt")
        assertTrue(cache.get("a.kt", "content").isEmpty())
    }

    @Test
    fun `invalidate does nothing for nonexistent key`() {
        cache.invalidate("nonexistent.kt")
        assertTrue(cache.get("nonexistent.kt", "content").isEmpty())
    }

    @Test
    fun `updateKey moves entry to new key`() {
        cache.put("old.kt", "content", listOf(createFn("fn1")))
        cache.updateKey("old.kt", "new.kt")
        assertTrue(cache.get("old.kt", "content").isEmpty())
        assertEquals(1, cache.get("new.kt", "content").size)
    }

    @Test
    fun `getLastKnown returns stored result even when content hash differs`() {
        cache.put("a.kt", "content", listOf(createFn("fn1")))

        assertTrue(cache.get("a.kt", "different content").isEmpty())
        assertEquals(1, cache.getLastKnown("a.kt").size)
        assertEquals("fn1", cache.getLastKnown("a.kt")[0].name)
    }

    @Test
    fun `getLastKnown returns empty list when there is no entry`() {
        assertTrue(cache.getLastKnown("a.kt").isEmpty())
    }

    @Test
    fun `getLastKnown returns latest result after put is called again`() {
        cache.put("a.kt", "v1", listOf(createFn("old")))
        cache.put("a.kt", "v2", listOf(createFn("new")))

        assertEquals("new", cache.getLastKnown("a.kt")[0].name)
    }

    @Test
    fun `getLastKnown returns empty list after invalidate`() {
        cache.put("a.kt", "content", listOf(createFn()))

        cache.invalidate("a.kt")

        assertTrue(cache.getLastKnown("a.kt").isEmpty())
    }

    @Test
    fun `query-based get works the same as convenience get`() {
        cache.put("a.kt", "content", listOf(createFn("fn1")))
        val query = AceRefactorableFunctionCacheQuery("a.kt", "content")
        val result = cache.get(query)
        assertEquals(1, result.size)
    }
}
