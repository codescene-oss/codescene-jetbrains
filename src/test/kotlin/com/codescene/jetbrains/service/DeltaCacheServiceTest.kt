package com.codescene.jetbrains.service

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.services.cache.DeltaCacheEntry
import com.codescene.jetbrains.services.cache.DeltaCacheQuery
import com.codescene.jetbrains.services.cache.DeltaCacheService
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DeltaCacheServiceTest {
    private lateinit var deltaCacheService: DeltaCacheService
    private lateinit var deltaApiResponse: Delta

    private val filePath = "/path/to/file.txt"
    private val headCommitContent = "original code"
    private val currentFileContent = "modified code"

    @Before
    fun setUp() {
        deltaCacheService = DeltaCacheService()
        deltaApiResponse = mockk()
    }

    @Test
    fun `put stores delta cache entry`() {
        val entry = DeltaCacheEntry(filePath, headCommitContent, currentFileContent, deltaApiResponse)

        deltaCacheService.put(entry)

        val cachedResponse = deltaCacheService.get(DeltaCacheQuery(filePath, headCommitContent, currentFileContent))
        assertEquals(deltaApiResponse, cachedResponse)
    }

    @Test
    fun `get returns null when cache does not exist`() {
        val cachedResponse = deltaCacheService.get(DeltaCacheQuery(filePath, headCommitContent, currentFileContent))

        assertNull(cachedResponse)
    }

    @Test
    fun `get returns null when content does not match`() {
        val entry = DeltaCacheEntry(filePath, headCommitContent, currentFileContent, deltaApiResponse)
        deltaCacheService.put(entry)

        val cachedResponse =
            deltaCacheService.get(DeltaCacheQuery(filePath, "different head content", currentFileContent))

        assertNull(cachedResponse)
    }
}