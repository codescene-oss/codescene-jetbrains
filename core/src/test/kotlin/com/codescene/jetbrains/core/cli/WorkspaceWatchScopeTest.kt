package com.codescene.jetbrains.core.cli

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceWatchScopeTest {
    private val watch = WorkspaceWatch(windows = false)

    @Test
    fun `new roots schedule one confirm each`() {
        val effects =
            watch.setScopes(
                mapOf(
                    "/a" to listOf("."),
                    "/b" to listOf("src"),
                ),
            )
        val confirms = effects.filterIsInstance<WatchEffect.ScheduleConfirm>()
        assertEquals(2, confirms.size)
        confirms.forEach { assertEquals(WorkspaceWatch.CONFIRM_DELAY_MS, it.delayMs) }
        assertNotEquals(confirms[0].token, confirms[1].token)
    }

    @Test
    fun `unchanged scopes emit nothing and changed paths schedule a new confirm`() {
        watch.setScopes(mapOf("/repo" to listOf("src")))
        assertEquals(emptyList<WatchEffect>(), watch.setScopes(mapOf("/repo" to listOf("src"))))
        val effects = watch.setScopes(mapOf("/repo" to listOf("src", "test")))
        assertEquals(1, effects.size)
        assertTrue(effects[0] is WatchEffect.ScheduleConfirm)
    }

    @Test
    fun `a removed root is no longer resolved or watched`() {
        watch.setScopes(mapOf("/repo" to listOf(".")))
        watch.inventoryPushed(WatchInventory("/repo", setOf("a.kt")))
        assertTrue(watch.isWatched("/repo"))
        assertEquals(ResolvedPath("/repo", "/repo/a.kt"), watch.resolve("/repo", "a.kt"))
        watch.setScopes(emptyMap())
        assertFalse(watch.isWatched("/repo"))
        assertNull(watch.resolve("/repo", "a.kt"))
    }

    @Test
    fun `only the latest confirm token fetches`() {
        val scopeConfirm =
            watch.setScopes(mapOf("/repo" to listOf("."))).single() as WatchEffect.ScheduleConfirm
        val sentConfirm = watch.watchSent("/repo").single() as WatchEffect.ScheduleConfirm
        assertEquals(emptyList<WatchEffect>(), watch.confirmDue("/repo", scopeConfirm.token))
        assertEquals(
            listOf(WatchEffect.FetchInventory("/repo", 0)),
            watch.confirmDue("/repo", sentConfirm.token),
        )
        assertEquals(emptyList<WatchEffect>(), watch.confirmDue("/repo", sentConfirm.token))
    }

    @Test
    fun `watchSent matches another root form on Windows and ignores unknown roots`() {
        val windowsWatch = WorkspaceWatch(windows = true)
        windowsWatch.setScopes(mapOf("C:/repo" to listOf(".")))
        val effect = windowsWatch.watchSent("c:\\Repo").single() as WatchEffect.ScheduleConfirm
        assertEquals("C:/repo", effect.repoRoot)
        assertEquals(emptyList<WatchEffect>(), windowsWatch.watchSent("/other"))
    }

    @Test
    fun `resolve maps in-scope paths to the local root`() {
        val windowsWatch = WorkspaceWatch(windows = true)
        windowsWatch.setScopes(mapOf("C:/repo" to listOf("src")))
        assertEquals(
            ResolvedPath("C:/repo", "C:/repo/src/A.kt"),
            windowsWatch.resolve("c:\\repo", "src\\A.kt"),
        )
        assertEquals(
            ResolvedPath("C:/repo", "C:/repo/SRC/A.kt"),
            windowsWatch.resolve("C:/repo", "SRC/A.kt"),
        )
        val posixWatch = WorkspaceWatch(windows = false)
        posixWatch.setScopes(mapOf("/repo" to listOf("src")))
        assertNull(posixWatch.resolve("/repo", "SRC/A.kt"))
        assertNull(windowsWatch.resolve("C:/repo", "test/A.kt"))
        assertNull(windowsWatch.resolve("C:/repo", "srcx/A.kt"))
        windowsWatch.setScopes(mapOf("C:/repo" to listOf(".")))
        assertEquals(
            ResolvedPath("C:/repo", "C:/repo/any/path.kt"),
            windowsWatch.resolve("C:/repo", "any/path.kt"),
        )
    }

    @Test
    fun `isWatched becomes true after the first push or fetch`() {
        watch.setScopes(mapOf("/repo" to listOf(".")))
        assertFalse(watch.isWatched("/repo"))
        watch.inventoryPushed(WatchInventory("/repo", setOf("a.kt")))
        assertTrue(watch.isWatched("/repo"))
        val fresh = WorkspaceWatch(windows = false)
        fresh.setScopes(mapOf("/repo" to listOf(".")))
        assertFalse(fresh.isWatched("/repo"))
        fresh.inventoryFetched(WatchInventory("/repo", setOf("b.kt")), 0)
        assertTrue(fresh.isWatched("/repo"))
    }
}
