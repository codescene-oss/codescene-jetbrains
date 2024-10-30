package com.codescene.jetbrains.service

import com.codescene.jetbrains.data.ApiResponse
import com.codescene.jetbrains.services.CacheEntry
import com.codescene.jetbrains.services.CacheQuery
import com.codescene.jetbrains.services.ReviewCacheService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

const val filePath = "/path/to/file.txt"
const val fileContents = "code"
const val newFileContents = "code changed"

class ReviewCacheServiceTest {
    private lateinit var reviewCacheService: ReviewCacheService
    private lateinit var response: ApiResponse

    @Before
    fun setUp() {
        reviewCacheService = ReviewCacheService()
        response = mock()
    }

    @Test
    fun `cacheResponse stores the response in cache`() {
        val entry = CacheEntry(fileContents, filePath, response)

        reviewCacheService.cacheResponse(entry)

        val cachedResponse = reviewCacheService.getCachedResponse(CacheQuery(fileContents, filePath))

        assertEquals(response, cachedResponse)
    }

    @Test
    fun `getCachedResponse returns null if no cache entry exists`() {
        val cachedResponse = reviewCacheService.getCachedResponse(CacheQuery(fileContents, filePath))

        assertNull(cachedResponse)
    }

    @Test
    fun `getCachedResponse returns null if file contents do not match`() {
        reviewCacheService.cacheResponse(CacheEntry(fileContents, filePath, response))

        val cachedResponse = reviewCacheService.getCachedResponse(CacheQuery(newFileContents, filePath))

        assertNull(cachedResponse)
    }
}