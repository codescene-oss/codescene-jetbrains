package com.codescene.jetbrains.core.git

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceActivityTest {
    @Test
    fun `activity is consumed once`() {
        val activity = WorkspaceActivity()
        activity.markWorkspaceFileActivity()

        assertTrue(activity.consumeWorkspaceFileActivity())
        assertFalse(activity.consumeWorkspaceFileActivity())
    }
}
