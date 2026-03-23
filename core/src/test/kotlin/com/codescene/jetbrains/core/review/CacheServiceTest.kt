package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.TestLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CacheServiceTest {
    private lateinit var cache: TestCacheService

    @Before
    fun setUp() {
        cache = TestCacheService()
    }

    @Test
    fun `get returns null when empty`() {
        assertNull(cache.get(TestQuery("a.kt", "content")))
    }

    @Test
    fun `put and get returns cached value`() {
        cache.put(TestEntry("a.kt", "content", "result"))
        assertEquals("result", cache.get(TestQuery("a.kt", "content")))
    }

    @Test
    fun `get returns null when content hash does not match`() {
        cache.put(TestEntry("a.kt", "content", "result"))
        assertNull(cache.get(TestQuery("a.kt", "different")))
    }

    @Test
    fun `invalidate removes entry`() {
        cache.put(TestEntry("a.kt", "content", "result"))
        cache.invalidate("a.kt")
        assertNull(cache.get(TestQuery("a.kt", "content")))
    }

    @Test
    fun `invalidate does nothing for missing key`() {
        cache.invalidate("missing.kt")
        assertNull(cache.get(TestQuery("missing.kt", "content")))
    }

    @Test
    fun `updateKey moves entry`() {
        cache.put(TestEntry("old.kt", "content", "result"))
        cache.updateKey("old.kt", "new.kt")
        assertNull(cache.get(TestQuery("old.kt", "content")))
        assertEquals("result", cache.get(TestQuery("new.kt", "content")))
    }

    @Test
    fun `updateKey does nothing for missing key`() {
        cache.updateKey("missing.kt", "new.kt")
        assertNull(cache.get(TestQuery("new.kt", "content")))
    }

    @Test
    fun `getAll returns all entries`() {
        cache.put(TestEntry("a.kt", "c1", "r1"))
        cache.put(TestEntry("b.kt", "c2", "r2"))
        val all = cache.getAll()
        assertEquals(2, all.size)
        assertTrue(all.any { it.first == "a.kt" })
        assertTrue(all.any { it.first == "b.kt" })
    }

    data class TestQuery(val filePath: String, val content: String)

    data class TestEntry(val filePath: String, val content: String, val result: String)

    data class TestItem(val contentHash: String, val result: String)

    class TestCacheService : CacheService<TestQuery, TestEntry, TestItem, String>(TestLogger) {
        override fun get(query: TestQuery): String? {
            val item = cache[query.filePath] ?: return null
            return if (item.contentHash == hash(query.content)) item.result else null
        }

        override fun put(entry: TestEntry) {
            cache[entry.filePath] = TestItem(hash(entry.content), entry.result)
        }
    }
}
