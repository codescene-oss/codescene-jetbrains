package com.codescene.jetbrains.platform.git

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodesceneRepoFilesListenerTest {
    private val listener =
        CodesceneRepoFilesListener(
            project = io.mockk.mockk(relaxed = true),
            gitRootPath = "/repo",
            observer = null,
            mainLineBranchResolver = io.mockk.mockk(relaxed = true),
        )

    @Test
    fun `isCodesceneConfigPath matches config json under codescene`() {
        assertTrue(listener.isCodesceneConfigPath("/repo/.codescene/config.json"))
        assertTrue(listener.isCodesceneConfigPath("C:\\repo\\.codescene\\config.json"))
        assertFalse(listener.isCodesceneConfigPath("/repo/.codescene/code-health-rules.json"))
    }

    @Test
    fun `isCodeHealthRulesPath matches rules json`() {
        assertTrue(listener.isCodeHealthRulesPath("/repo/.codescene/code-health-rules.json"))
        assertFalse(listener.isCodeHealthRulesPath("/repo/.codescene/config.json"))
    }
}
