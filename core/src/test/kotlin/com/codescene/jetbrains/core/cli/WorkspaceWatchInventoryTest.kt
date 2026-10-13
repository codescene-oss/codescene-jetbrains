package com.codescene.jetbrains.core.cli

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceWatchInventoryTest {
    private val watch = WorkspaceWatch(windows = false)

    @Test
    fun `a push replaces the inventory filtered to the scope`() {
        watch.setScopes(mapOf("/repo" to listOf("src")))
        val first =
            watch.inventoryPushed(
                WatchInventory("/repo", setOf("src/A.kt", "test/B.kt")),
            )
        assertEquals(
            listOf(WatchEffect.SyncMonitor("/repo", setOf("/repo/src/A.kt"))),
            first,
        )
        assertEquals(setOf("src/A.kt"), watch.inventory("/repo"))
        watch.inventoryPushed(WatchInventory("/repo", setOf("src/C.kt")))
        assertEquals(setOf("src/C.kt"), watch.inventory("/repo"))
    }

    @Test
    fun `a push clears the pending confirm`() {
        watch.setScopes(mapOf("/repo" to listOf(".")))
        val token = (watch.watchSent("/repo").single() as WatchEffect.ScheduleConfirm).token
        watch.inventoryPushed(WatchInventory("/repo", setOf("a.kt")))
        assertEquals(emptyList<WatchEffect>(), watch.confirmDue("/repo", token))
    }

    @Test
    fun `an empty push syncs and then fetches`() {
        watch.setScopes(mapOf("/repo" to listOf(".")))
        val effects = watch.inventoryPushed(WatchInventory("/repo", emptySet()))
        assertEquals(
            listOf(
                WatchEffect.SyncMonitor("/repo", emptySet()),
                WatchEffect.FetchInventory("/repo", 1),
            ),
            effects,
        )
    }

    @Test
    fun `a fetch is applied only when no push arrived since it was issued`() {
        watch.setScopes(mapOf("/repo" to listOf(".")))
        val sentConfirm = watch.watchSent("/repo").single() as WatchEffect.ScheduleConfirm
        assertEquals(
            listOf(WatchEffect.FetchInventory("/repo", 0)),
            watch.confirmDue("/repo", sentConfirm.token),
        )
        watch.inventoryFetched(WatchInventory("/repo", setOf("a.kt")), 0)
        assertEquals(setOf("a.kt"), watch.inventory("/repo"))
        val nextConfirm = watch.watchSent("/repo").single() as WatchEffect.ScheduleConfirm
        assertEquals(
            listOf(WatchEffect.FetchInventory("/repo", 0)),
            watch.confirmDue("/repo", nextConfirm.token),
        )
        watch.inventoryPushed(WatchInventory("/repo", setOf("b.kt")))
        assertEquals(emptyList<WatchEffect>(), watch.inventoryFetched(WatchInventory("/repo", setOf("x.kt")), 0))
        assertEquals(setOf("b.kt"), watch.inventory("/repo"))
        val emptyPush = watch.inventoryPushed(WatchInventory("/repo", emptySet()))
        assertEquals(WatchEffect.FetchInventory("/repo", 2), emptyPush.last())
    }

    @Test
    fun `a not-watched fetch failure reports the root as lost and keeps the inventory`() {
        watch.setScopes(mapOf("/repo" to listOf(".")))
        watch.inventoryPushed(WatchInventory("/repo", setOf("keep.kt")))
        assertEquals(
            listOf(WatchEffect.ReportLost("/repo")),
            watch.inventoryFetchFailed("/repo", notWatched = true),
        )
        assertEquals(setOf("keep.kt"), watch.inventory("/repo"))
        assertEquals(emptyList<WatchEffect>(), watch.inventoryFetchFailed("/repo", notWatched = false))
    }

    @Test
    fun `HEAD tracking`() {
        watch.setScopes(mapOf("/repo" to listOf(".")))
        assertEquals(emptyList<WatchEffect>(), watch.headObserved("/repo", "main", "c1"))
        assertEquals(emptyList<WatchEffect>(), watch.headObserved("/repo", "main", "c1"))
        val branchChange = watch.headObserved("/repo", "feature", "c1")
        assertEquals(2, branchChange.size)
        assertEquals(WatchEffect.Reseed("/repo"), branchChange[0])
        val branchConfirm = branchChange[1] as WatchEffect.ScheduleConfirm
        assertEquals("/repo", branchConfirm.repoRoot)
        assertEquals(WorkspaceWatch.CONFIRM_DELAY_MS, branchConfirm.delayMs)
        val revisionChange = watch.headObserved("/repo", "feature", "c2")
        assertEquals(2, revisionChange.size)
        assertEquals(WatchEffect.Reseed("/repo"), revisionChange[0])
        val revisionConfirm = revisionChange[1] as WatchEffect.ScheduleConfirm
        assertEquals("/repo", revisionConfirm.repoRoot)
        assertEquals(WorkspaceWatch.CONFIRM_DELAY_MS, revisionConfirm.delayMs)
        assertEquals(emptyList<WatchEffect>(), watch.headObserved("/other", "main", "c1"))
    }

    @Test
    fun `progressIdle syncs every root with an inventory`() {
        watch.setScopes(mapOf("/a" to listOf("."), "/b" to listOf(".")))
        watch.inventoryPushed(WatchInventory("/a", setOf("x.kt")))
        val effects = watch.progressIdle()
        assertEquals(1, effects.size)
        assertEquals(WatchEffect.SyncMonitor("/a", setOf("/a/x.kt")), effects.single())
    }

    @Test
    fun `a push for a root outside the scopes emits nothing`() {
        watch.setScopes(mapOf("/repo" to listOf(".")))
        assertEquals(emptyList<WatchEffect>(), watch.inventoryPushed(WatchInventory("/other", setOf("a.kt"))))
    }
}
