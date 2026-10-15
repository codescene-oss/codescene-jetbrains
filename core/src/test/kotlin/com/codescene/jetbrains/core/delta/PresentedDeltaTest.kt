package com.codescene.jetbrains.core.delta

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.TestLogger
import com.codescene.jetbrains.core.testdoubles.InMemoryDeltaCacheService
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PresentedDeltaTest {
    private lateinit var deltaCacheService: DeltaCacheService

    private val filePath = "/repo/src/file.kt"

    @Before
    fun setUp() {
        deltaCacheService = DeltaCacheService(TestLogger)
    }

    @Test
    fun `equal blob SHAs give equal head and current text`() {
        assertEquals("text" to "text", presentedDeltaContents("text", "aaa", "aaa"))
    }

    @Test
    fun `different blob SHAs give a baseline head and the scored text`() {
        assertEquals("baseline:aaa" to "text", presentedDeltaContents("text", "aaa", "bbb"))
    }

    @Test
    fun `missing scored text with equal SHAs uses the new blob for both`() {
        assertEquals("blob:abc" to "blob:abc", presentedDeltaContents(null, "abc", "abc"))
    }

    @Test
    fun `a file absent from the baseline gets an empty baseline head`() {
        assertEquals("baseline:" to "blob:abc", presentedDeltaContents(null, null, "abc"))
    }

    @Test
    fun `missing scored text with a different old SHA uses a baseline head`() {
        assertEquals("baseline:old" to "blob:abc", presentedDeltaContents(null, "old", "abc"))
    }

    @Test
    fun `all SHAs missing give equal head and current text`() {
        assertEquals("blob:" to "blob:", presentedDeltaContents(null, null, null))
    }

    @Test
    fun `getByCurrentContent hits on the current text when the SHAs differed`() {
        val delta = zeroDelta()
        putPresented("text", "aaa", "bbb", delta)

        val (hit, cached) = deltaCacheService.getByCurrentContent(filePath, "text")

        assertTrue(hit)
        assertEquals(delta, cached)
    }

    @Test
    fun `getByCurrentContent misses on different current text`() {
        putPresented("text", "aaa", "bbb", zeroDelta())

        val (hit, cached) = deltaCacheService.getByCurrentContent(filePath, "other")

        assertFalse(hit)
        assertNull(cached)
    }

    @Test
    fun `getByCurrentContent misses when no entry exists`() {
        val (hit, cached) = deltaCacheService.getByCurrentContent(filePath, "text")

        assertFalse(hit)
        assertNull(cached)
    }

    @Test
    fun `getByCurrentContent matches across path separator differences`() {
        val delta = zeroDelta()
        val (head, current) = presentedDeltaContents("text", "aaa", "bbb")
        deltaCacheService.put(DeltaCacheEntry("C:\\repo\\src\\file.kt", head, current, delta))

        val (hit, cached) = deltaCacheService.getByCurrentContent("C:/repo/src/file.kt", "text")

        assertTrue(hit)
        assertEquals(delta, cached)
    }

    @Test
    fun `getAll includes a zero score change when the SHAs differed`() {
        putPresented("text", "aaa", "bbb", zeroDelta())

        val all = deltaCacheService.getAll()

        assertEquals(1, all.size)
        assertEquals(filePath, all[0].first)
    }

    @Test
    fun `getAll excludes a zero score change when the SHAs were equal`() {
        putPresented("text", "aaa", "aaa", zeroDelta())

        assertEquals(0, deltaCacheService.getAll().size)
    }

    @Test
    fun `null delta is a hit and absent from getAll`() {
        putPresented("text", "aaa", "bbb", null)

        val (hit, cached) = deltaCacheService.getByCurrentContent(filePath, "text")

        assertTrue(hit)
        assertNull(cached)
        assertEquals(0, deltaCacheService.getAll().size)
    }

    @Test
    fun `in-memory cache delegates getByCurrentContent`() {
        val inMemory = InMemoryDeltaCacheService()
        val delta = zeroDelta()
        val (head, current) = presentedDeltaContents("text", "aaa", "bbb")
        inMemory.put(DeltaCacheEntry(filePath, head, current, delta))

        assertEquals(true to delta, inMemory.getByCurrentContent(filePath, "text"))
        assertEquals(false to null, inMemory.getByCurrentContent(filePath, "other"))
    }

    private fun putPresented(
        scoredText: String?,
        oldGitBlobSha: String?,
        newGitBlobSha: String?,
        delta: Delta?,
    ) {
        val (head, current) = presentedDeltaContents(scoredText, oldGitBlobSha, newGitBlobSha)
        deltaCacheService.put(DeltaCacheEntry(filePath, head, current, delta))
    }

    private fun zeroDelta(): Delta {
        val delta = mockk<Delta>()
        every { delta.scoreChange } returns 0.0
        every { delta.fileLevelFindings } returns emptyList()
        every { delta.functionLevelFindings } returns emptyList()
        return delta
    }
}
