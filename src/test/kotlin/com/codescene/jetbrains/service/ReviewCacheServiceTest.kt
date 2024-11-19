package com.codescene.jetbrains.service

import com.codescene.jetbrains.data.CodeReview
import com.codescene.jetbrains.services.cache.ReviewCacheEntry
import com.codescene.jetbrains.services.cache.ReviewCacheQuery
import com.codescene.jetbrains.services.cache.ReviewCacheService
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

const val filePath = "/path/to/file.txt"
const val fileContents = "code"
const val newFileContents = "code changed"

class ReviewCacheServiceTest {
    private lateinit var reviewCacheService: ReviewCacheService
    private lateinit var response: CodeReview

    @Before
    fun setUp() {
        reviewCacheService = ReviewCacheService()
        response = mockk()
    }

    @Test
    fun `cacheResponse stores the response in cache`() {
        val entry = ReviewCacheEntry(fileContents, filePath, response)

        reviewCacheService.cacheResponse(entry)

        val cachedResponse = reviewCacheService.getCachedResponse(ReviewCacheQuery(fileContents, filePath))

        assertEquals(response, cachedResponse)
    }

    @Test
    fun `getCachedResponse returns null if no cache entry exists`() {
        val cachedResponse = reviewCacheService.getCachedResponse(ReviewCacheQuery(fileContents, filePath))

        assertNull(cachedResponse)
    }

    @Test
    fun `getCachedResponse returns null if file contents do not match`() {
        reviewCacheService.cacheResponse(ReviewCacheEntry(fileContents, filePath, response))

        val cachedResponse = reviewCacheService.getCachedResponse(ReviewCacheQuery(newFileContents, filePath))

        assertNull(cachedResponse)
    }
}