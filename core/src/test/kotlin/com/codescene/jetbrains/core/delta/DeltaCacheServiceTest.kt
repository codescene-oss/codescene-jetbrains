package com.codescene.jetbrains.core.delta

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.TestLogger
import io.mockk.every
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
        deltaCacheService = DeltaCacheService(TestLogger)
        deltaApiResponse = mockk()
    }

    @Test
    fun `put stores delta cache entry`() {
        val entry = DeltaCacheEntry(filePath, headCommitContent, currentFileContent, deltaApiResponse)

        deltaCacheService.put(entry)

        val cachedResponse = deltaCacheService.get(DeltaCacheQuery(filePath, headCommitContent, currentFileContent))
        assertEquals(deltaApiResponse, cachedResponse.second)
    }

    @Test
    fun `get returns null when cache does not exist`() {
        val cachedResponse = deltaCacheService.get(DeltaCacheQuery(filePath, headCommitContent, currentFileContent))

        assertEquals(false, cachedResponse.first)
        assertNull(cachedResponse.second)
    }

    @Test
    fun `get returns null when content does not match`() {
        val entry = DeltaCacheEntry(filePath, headCommitContent, currentFileContent, deltaApiResponse)
        deltaCacheService.put(entry)

        val cachedResponse =
            deltaCacheService.get(DeltaCacheQuery(filePath, "different head content", currentFileContent))

        assertEquals(false, cachedResponse.first)
    }

    @Test
    fun `get returns entry when content matches`() {
        val entry = DeltaCacheEntry(filePath, headCommitContent, currentFileContent, deltaApiResponse)
        deltaCacheService.put(entry)

        val cachedResponse =
            deltaCacheService.get(DeltaCacheQuery(filePath, headCommitContent, currentFileContent))

        assertEquals(true, cachedResponse.first)
        assertEquals(deltaApiResponse, cachedResponse.second)
    }

    @Test
    fun `get returns cache hit for cached null delta`() {
        deltaCacheService.put(DeltaCacheEntry(filePath, headCommitContent, currentFileContent, null))

        val cachedResponse =
            deltaCacheService.get(DeltaCacheQuery(filePath, headCommitContent, currentFileContent))

        assertEquals(true, cachedResponse.first)
        assertNull(cachedResponse.second)
    }

    @Test
    fun `getAll excludes unchanged code with zero score change`() {
        val delta = mockk<Delta>()
        every { delta.scoreChange } returns 0.0
        deltaCacheService.put(DeltaCacheEntry(filePath, "same", "same", delta))

        assertEquals(0, deltaCacheService.getAll().size)
    }

    @Test
    fun `getAll includes code change with zero score change`() {
        val delta = mockk<Delta>()
        every { delta.scoreChange } returns 0.0
        deltaCacheService.put(DeltaCacheEntry(filePath, headCommitContent, currentFileContent, delta))

        val all = deltaCacheService.getAll()
        assertEquals(1, all.size)
        assertEquals(filePath, all[0].first)
    }

    @Test
    fun `getAll includes score change when head and current content match`() {
        val delta = mockk<Delta>()
        every { delta.scoreChange } returns -0.5
        deltaCacheService.put(DeltaCacheEntry(filePath, "same", "same", delta))

        val all = deltaCacheService.getAll()
        assertEquals(1, all.size)
        assertEquals(filePath, all[0].first)
    }

    @Test
    fun `getAll excludes null delta response`() {
        deltaCacheService.put(DeltaCacheEntry(filePath, headCommitContent, currentFileContent, null))

        assertEquals(0, deltaCacheService.getAll().size)
    }

    @Test
    fun `getAll excludes entries with includeInCodeHealthMonitor false`() {
        val delta = mockk<Delta>()
        every { delta.scoreChange } returns 1.0
        deltaCacheService.put(
            DeltaCacheEntry(
                filePath,
                headCommitContent,
                currentFileContent,
                delta,
                includeInCodeHealthMonitor = false,
            ),
        )

        assertEquals(0, deltaCacheService.getAll().size)
    }

    @Test
    fun `setIncludeInCodeHealthMonitor toggles getAll visibility`() {
        val delta = mockk<Delta>()
        every { delta.scoreChange } returns 1.0
        deltaCacheService.put(DeltaCacheEntry(filePath, headCommitContent, currentFileContent, delta))

        assertEquals(1, deltaCacheService.getAll().size)

        deltaCacheService.setIncludeInCodeHealthMonitor(filePath, false)

        assertEquals(0, deltaCacheService.getAll().size)
    }

    @Test
    fun `setIncludeInCodeHealthMonitor restores visible cache entry`() {
        val delta = mockk<Delta>()
        every { delta.scoreChange } returns 1.0
        deltaCacheService.put(DeltaCacheEntry(filePath, headCommitContent, currentFileContent, delta))

        deltaCacheService.setIncludeInCodeHealthMonitor(filePath, false)
        deltaCacheService.setIncludeInCodeHealthMonitor(filePath, true)

        assertEquals(1, deltaCacheService.getAll().size)
    }

    @Test
    fun `cache operations match Windows paths across separator differences`() {
        val delta = mockk<Delta>()
        every { delta.scoreChange } returns 1.0
        val backslashPath = "C:\\repo\\src\\file.kt"
        val slashPath = "C:/repo/src/file.kt"
        deltaCacheService.put(DeltaCacheEntry(backslashPath, headCommitContent, currentFileContent, delta))

        val cachedResponse =
            deltaCacheService.get(DeltaCacheQuery(slashPath, headCommitContent, currentFileContent))

        assertEquals(true, cachedResponse.first)
        assertEquals(delta, cachedResponse.second)

        deltaCacheService.setIncludeInCodeHealthMonitor(slashPath, false)
        assertEquals(0, deltaCacheService.getAll().size)

        deltaCacheService.setIncludeInCodeHealthMonitor(slashPath, true)
        val all = deltaCacheService.getAll()
        assertEquals(1, all.size)
        assertEquals(backslashPath, all[0].first)
    }
}
