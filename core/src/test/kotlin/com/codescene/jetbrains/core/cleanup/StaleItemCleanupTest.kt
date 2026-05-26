package com.codescene.jetbrains.core.cleanup

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.TestLogger
import com.codescene.jetbrains.core.testdoubles.InMemoryDeltaCacheService
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StaleItemCleanupTest {
    private lateinit var deltaCacheService: InMemoryDeltaCacheService
    private lateinit var staleItemCleanup: StaleItemCleanup

    @Before
    fun setUp() {
        deltaCacheService = InMemoryDeltaCacheService()
        staleItemCleanup = StaleItemCleanup(deltaCacheService, TestLogger)
    }

    private fun createDelta(scoreChange: Double = 1.0): Delta {
        val delta = mockk<Delta>()
        every { delta.scoreChange } returns scoreChange
        return delta
    }

    private fun addToCache(filePath: String) {
        deltaCacheService.put(
            com.codescene.jetbrains.core.delta.DeltaCacheEntry(
                filePath = filePath,
                headContent = "original",
                currentFileContent = "modified",
                deltaApiResponse = createDelta(),
            ),
        )
    }

    @Test
    fun `stale item is removed when not in git changes AND not in visible editors`() {
        addToCache("/project/src/stale.kt")

        val removed =
            staleItemCleanup.cleanupStaleItems(
                gitChangedFiles = emptySet(),
                visibleEditorFiles = emptySet(),
            )

        assertEquals(1, removed.size)
        assertEquals("/project/src/stale.kt", removed[0])
        assertEquals(0, deltaCacheService.getAll().size)
    }

    @Test
    fun `item is NOT removed when present in git changes`() {
        val filePath = "/project/src/changed.kt"
        addToCache(filePath)

        val removed =
            staleItemCleanup.cleanupStaleItems(
                gitChangedFiles = setOf(filePath),
                visibleEditorFiles = emptySet(),
            )

        assertEquals(0, removed.size)
        assertEquals(1, deltaCacheService.getAll().size)
    }

    @Test
    fun `item is NOT removed when present in visible editors`() {
        val filePath = "/project/src/open.kt"
        addToCache(filePath)

        val removed =
            staleItemCleanup.cleanupStaleItems(
                gitChangedFiles = emptySet(),
                visibleEditorFiles = setOf(filePath),
            )

        assertEquals(0, removed.size)
        assertEquals(1, deltaCacheService.getAll().size)
    }

    @Test
    fun `item is NOT removed when present in both git changes and visible editors`() {
        val filePath = "/project/src/both.kt"
        addToCache(filePath)

        val removed =
            staleItemCleanup.cleanupStaleItems(
                gitChangedFiles = setOf(filePath),
                visibleEditorFiles = setOf(filePath),
            )

        assertEquals(0, removed.size)
        assertEquals(1, deltaCacheService.getAll().size)
    }

    @Test
    fun `path comparison handles Windows backslashes vs Unix forward slashes`() {
        addToCache("C:\\project\\src\\file.kt")

        val removed =
            staleItemCleanup.cleanupStaleItems(
                gitChangedFiles = setOf("C:/project/src/file.kt"),
                visibleEditorFiles = emptySet(),
            )

        assertEquals(0, removed.size)
        assertEquals(1, deltaCacheService.getAll().size)
    }

    @Test
    fun `multiple items - only stale ones are removed`() {
        addToCache("/project/src/kept1.kt")
        addToCache("/project/src/kept2.kt")
        addToCache("/project/src/stale1.kt")
        addToCache("/project/src/stale2.kt")

        val removed =
            staleItemCleanup.cleanupStaleItems(
                gitChangedFiles = setOf("/project/src/kept1.kt"),
                visibleEditorFiles = setOf("/project/src/kept2.kt"),
            )

        assertEquals(2, removed.size)
        assertTrue(removed.contains("/project/src/stale1.kt"))
        assertTrue(removed.contains("/project/src/stale2.kt"))
        assertEquals(2, deltaCacheService.getAll().size)
    }

    @Test
    fun `empty cache produces no errors`() {
        val removed =
            staleItemCleanup.cleanupStaleItems(
                gitChangedFiles = setOf("/project/src/file.kt"),
                visibleEditorFiles = setOf("/project/src/other.kt"),
            )

        assertEquals(0, removed.size)
    }

    @Test
    fun `cleanup returns list of removed file paths`() {
        addToCache("/project/src/first.kt")
        addToCache("/project/src/second.kt")

        val removed =
            staleItemCleanup.cleanupStaleItems(
                gitChangedFiles = emptySet(),
                visibleEditorFiles = emptySet(),
            )

        assertEquals(2, removed.size)
        assertTrue(removed.contains("/project/src/first.kt"))
        assertTrue(removed.contains("/project/src/second.kt"))
    }

    @Test
    fun `item kept by visible editor with different path separator than cache`() {
        addToCache("C:\\project\\src\\editor.kt")

        val removed =
            staleItemCleanup.cleanupStaleItems(
                gitChangedFiles = emptySet(),
                visibleEditorFiles = setOf("C:/project/src/editor.kt"),
            )

        assertEquals(0, removed.size)
        assertEquals(1, deltaCacheService.getAll().size)
    }
}
