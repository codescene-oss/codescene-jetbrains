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

class ReviewCacheServiceTest {
    private lateinit var reviewCacheService: ReviewCacheService
    private lateinit var response: CodeReview

    private val filePath = "/path/to/file.txt"
    private val fileContents = "code"
    private val newFileContents = "code changed"

    @Before
    fun setUp() {
        reviewCacheService = ReviewCacheService()
        response = mockk()
    }

    @Test
    fun `cacheResponse stores the response in cache`() {
        val entry = ReviewCacheEntry(fileContents, filePath, response)

        reviewCacheService.put(entry)

        val cachedResponse = reviewCacheService.get(ReviewCacheQuery(fileContents, filePath))

        assertEquals(response, cachedResponse)
    }

    @Test
    fun `getCachedResponse returns null if no cache entry exists`() {
        val cachedResponse = reviewCacheService.get(ReviewCacheQuery(fileContents, filePath))

        assertNull(cachedResponse)
    }

    @Test
    fun `getCachedResponse returns null if file contents do not match`() {
        reviewCacheService.put(ReviewCacheEntry(fileContents, filePath, response))

        val cachedResponse = reviewCacheService.get(ReviewCacheQuery(newFileContents, filePath))

        assertNull(cachedResponse)
    }

    @Test
    fun `getCachedResponse returns null if file path does not match`() {
        val wrongFilePath = "/wrong/path/to/file.txt"
        reviewCacheService.put(ReviewCacheEntry(fileContents, filePath, response))

        val cachedResponse = reviewCacheService.get(ReviewCacheQuery(fileContents, wrongFilePath))

        assertNull(cachedResponse)
    }

    @Test
    fun `invalidate removes cache entry and get returns null`() {
        val entry = ReviewCacheEntry(fileContents, filePath, response)
        reviewCacheService.put(entry)

        reviewCacheService.invalidate(filePath)

        val cachedResponse = reviewCacheService.get(ReviewCacheQuery(fileContents, filePath))
        assertNull(cachedResponse)
    }

    @Test
    fun `invalidate does nothing if key does not exist`() {
        val cachedResponseBefore = reviewCacheService.get(ReviewCacheQuery(fileContents, filePath))
        assertNull(cachedResponseBefore)

        reviewCacheService.invalidate(filePath)

        val cachedResponseAfter = reviewCacheService.get(ReviewCacheQuery(fileContents, filePath))
        assertNull(cachedResponseAfter)
    }

    @Test
    fun `updateKey moves cache entry to new key and invalidates old key`() {
        val newFilePath = "/path/to/renamed_file.txt"
        reviewCacheService.put(ReviewCacheEntry(fileContents, filePath, response))

        assertEquals(response, reviewCacheService.get(ReviewCacheQuery(fileContents, filePath)))

        reviewCacheService.updateKey(filePath, newFilePath)

        assertNull(reviewCacheService.get(ReviewCacheQuery(fileContents, filePath)))

        assertEquals(response, reviewCacheService.get(ReviewCacheQuery(fileContents, newFilePath)))
    }

    @Test
    fun `updateKey does nothing if old key does not exist`() {
        val newFilePath = "/path/to/renamed_file.txt"

        assertNull(reviewCacheService.get(ReviewCacheQuery(fileContents, filePath)))
        assertNull(reviewCacheService.get(ReviewCacheQuery(fileContents, newFilePath)))

        reviewCacheService.updateKey(filePath, newFilePath)

        assertNull(reviewCacheService.get(ReviewCacheQuery(fileContents, filePath)))
        assertNull(reviewCacheService.get(ReviewCacheQuery(fileContents, newFilePath)))
    }
}