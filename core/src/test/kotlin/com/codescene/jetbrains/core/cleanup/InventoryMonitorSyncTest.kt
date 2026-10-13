package com.codescene.jetbrains.core.cleanup

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.TestLogger
import com.codescene.jetbrains.core.delta.DeltaCacheEntry
import com.codescene.jetbrains.core.testdoubles.InMemoryDeltaCacheService
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InventoryMonitorSyncTest {
    private lateinit var deltaCacheService: InMemoryDeltaCacheService
    private lateinit var sync: InventoryMonitorSync

    @Before
    fun setUp() {
        deltaCacheService = InMemoryDeltaCacheService()
        sync = InventoryMonitorSync(deltaCacheService, TestLogger)
    }

    private fun createDelta(scoreChange: Double = 1.0): Delta {
        val delta = mockk<Delta>()
        every { delta.scoreChange } returns scoreChange
        return delta
    }

    private fun addToCache(filePath: String) {
        deltaCacheService.put(
            DeltaCacheEntry(
                filePath = filePath,
                headContent = "original",
                currentFileContent = "modified",
                deltaApiResponse = createDelta(),
            ),
        )
    }

    @Test
    fun `hides entries under the root outside inventory and open files`() {
        addToCache("/repo/a.kt")
        addToCache("/repo/b.kt")
        addToCache("/repo/c.kt")
        val hidden =
            sync.sync(
                repoRoot = "/repo",
                inventoryPaths = setOf("/repo/a.kt"),
                openPaths = setOf("/repo/b.kt"),
            )
        assertEquals(1, hidden)
        val visible = deltaCacheService.getAll().map { it.first }.toSet()
        assertEquals(setOf("/repo/a.kt", "/repo/b.kt"), visible)
    }

    @Test
    fun `leaves other roots and sibling prefixes alone`() {
        addToCache("/other/x.kt")
        addToCache("/repo2/y.kt")
        val hidden =
            sync.sync(
                repoRoot = "/repo",
                inventoryPaths = emptySet(),
                openPaths = emptySet(),
            )
        assertEquals(0, hidden)
        assertEquals(2, deltaCacheService.getAll().size)
    }

    @Test
    fun `re-includes a hidden entry that is back in the inventory`() {
        val path = "/repo/a.kt"
        addToCache(path)
        sync.sync(repoRoot = "/repo", inventoryPaths = emptySet(), openPaths = emptySet())
        assertEquals(0, deltaCacheService.getAll().size)
        sync.sync(repoRoot = "/repo", inventoryPaths = setOf(path), openPaths = emptySet())
        assertEquals(1, deltaCacheService.getAll().size)
        assertEquals(path, deltaCacheService.getAll().single().first)
    }

    @Test
    fun `compares Windows paths by cache key`() {
        addToCache("C:/Repo/src/A.kt")
        sync.sync(
            repoRoot = "c:\\repo",
            inventoryPaths = setOf("C:\\Repo\\src\\A.kt"),
            openPaths = emptySet(),
        )
        assertEquals(1, deltaCacheService.getAll().size)
    }

    @Test
    fun `inventory paths without a cache entry create nothing`() {
        val hidden =
            sync.sync(
                repoRoot = "/repo",
                inventoryPaths = setOf("/repo/new.kt"),
                openPaths = emptySet(),
            )
        assertEquals(0, hidden)
        assertEquals(0, deltaCacheService.getAll().size)
    }
}
