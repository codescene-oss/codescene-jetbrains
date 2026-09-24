package com.codescene.jetbrains.core.git

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangedFilesScanStateTest {
    @Test
    fun `idle scan is skipped when there was no activity and a previous scan exists`() {
        val state = ChangedFilesScanState()
        state.recordScan("a\u0000b")

        assertTrue(state.shouldSkipIdleScan(hadWorkspaceActivity = false))
    }

    @Test
    fun `idle scan runs after workspace activity`() {
        val state = ChangedFilesScanState()
        state.recordScan("a\u0000b")

        assertFalse(state.shouldSkipIdleScan(hadWorkspaceActivity = true))
    }

    @Test
    fun `markDirty forces scan even when idle`() {
        val state = ChangedFilesScanState()
        state.recordScan("a\u0000b")
        state.markDirty()

        assertFalse(state.shouldSkipIdleScan(hadWorkspaceActivity = false))
    }

    @Test
    fun `unchanged file set is detected`() {
        val state = ChangedFilesScanState()
        state.recordScan(serializeChangedFileSet(setOf("/a.ts", "/b.ts")))

        assertTrue(state.isUnchangedFileSet(serializeChangedFileSet(setOf("/b.ts", "/a.ts"))))
    }

    @Test
    fun `sortFilesByPriority puts visible files first`() {
        val sorted =
            sortFilesByPriority(
                setOf("/hidden.ts", "/visible.ts"),
                setOf("/visible.ts"),
            )

        assertEquals(listOf("/visible.ts", "/hidden.ts"), sorted)
    }
}
