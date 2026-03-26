package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.TestLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BaselineReviewCacheServiceTest {
    private lateinit var cache: BaselineReviewCacheService

    private val filePath = "/path/to/file.kt"
    private val fileContents = "baseline"
    private val updatedContents = "changed"

    @Before
    fun setUp() {
        cache = BaselineReviewCacheService(TestLogger)
    }

    @Test
    fun `get returns miss when entry does not exist`() {
        val (found, score) = cache.get(BaselineReviewCacheQuery(fileContents, filePath))
        assertFalse(found)
        assertNull(score)
    }

    @Test
    fun `put stores score and get returns hit`() {
        cache.put(BaselineReviewCacheEntry(fileContents, filePath, 8.5))

        val (found, score) = cache.get(BaselineReviewCacheQuery(fileContents, filePath))

        assertTrue(found)
        assertEquals(8.5, score)
    }

    @Test
    fun `get returns miss when contents do not match`() {
        cache.put(BaselineReviewCacheEntry(fileContents, filePath, 8.5))

        val (found, _) = cache.get(BaselineReviewCacheQuery(updatedContents, filePath))

        assertFalse(found)
    }

    @Test
    fun `cache can store null score as hit`() {
        cache.put(BaselineReviewCacheEntry(fileContents, filePath, null))

        val (found, score) = cache.get(BaselineReviewCacheQuery(fileContents, filePath))

        assertTrue(found)
        assertNull(score)
    }
}
