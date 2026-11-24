package com.codescene.jetbrains.core.review

import com.codescene.data.review.Review
import com.codescene.jetbrains.core.delta.DeltaCacheEntry
import com.codescene.jetbrains.core.testdoubles.InMemoryDeltaCacheService
import com.codescene.jetbrains.core.testdoubles.InMemoryReviewCacheService
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewCacheProbeTest {
    @Test
    fun `isFullyCached returns true when review and delta caches match current content`() {
        val reviewCache = InMemoryReviewCacheService()
        val deltaCache = InMemoryDeltaCacheService()
        val path = "/workspace/src/file.ts"
        val currentCode = "current"
        val baselineCode = "baseline"
        val review = mockk<Review>()

        reviewCache.put(
            ReviewCacheEntry(
                fileContents = currentCode,
                filePath = path,
                response = review,
            ),
        )
        deltaCache.put(
            DeltaCacheEntry(
                filePath = path,
                headContent = baselineCode,
                currentFileContent = currentCode,
                deltaApiResponse = null,
            ),
        )

        assertTrue(
            ReviewCacheProbe.isFullyCached(path, currentCode, reviewCache, deltaCache),
        )
    }

    @Test
    fun `isFullyCached returns false when delta cache content changed`() {
        val reviewCache = InMemoryReviewCacheService()
        val deltaCache = InMemoryDeltaCacheService()
        val path = "/workspace/src/file.ts"
        val currentCode = "current"
        val review = mockk<Review>()

        reviewCache.put(
            ReviewCacheEntry(
                fileContents = currentCode,
                filePath = path,
                response = review,
            ),
        )
        deltaCache.put(
            DeltaCacheEntry(
                filePath = path,
                headContent = "baseline",
                currentFileContent = "old-current",
                deltaApiResponse = null,
            ),
        )

        assertFalse(
            ReviewCacheProbe.isFullyCached(path, currentCode, reviewCache, deltaCache),
        )
    }
}
