package com.codescene.jetbrains

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettings
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CodeSceneGlobalSettingsStoreTest : BasePlatformTestCase() {
    private lateinit var settingsStore: CodeSceneGlobalSettingsStore
    private var defaultServerUrl = "https://codescene.io"

    override fun setUp() {
        super.setUp()

        settingsStore = CodeSceneGlobalSettingsStore()
    }

    fun `test default state is initialized correctly`() {
        assertDefaultState(settingsStore.state)
    }

    fun `test loadState updates the current state`() {
        val newState = createCustomState()

        settingsStore.loadState(newState)

        assertStateEquals(newState, settingsStore.state)
    }

    fun `test getState returns the current state`() {
        val currentState = settingsStore.state

        assertNotNull(currentState)
        assertEquals(defaultServerUrl, currentState.serverUrl)
    }

    private fun assertStateEquals(expected: CodeSceneGlobalSettings, actual: CodeSceneGlobalSettings) {
        assertEquals(expected.foldingRangeProvider, actual.foldingRangeProvider)
        assertEquals(expected.defaultEditorFormatter, actual.defaultEditorFormatter)
        assertEquals(expected.defaultNotebookFormatter, actual.defaultNotebookFormatter)
        assertEquals(expected.serverUrl, actual.serverUrl)
        assertEquals(expected.enableCodeLenses, actual.enableCodeLenses)
        assertEquals(expected.enableAutoRefactor, actual.enableAutoRefactor)
        assertEquals(expected.excludeGitignoreFiles, actual.excludeGitignoreFiles)
        assertEquals(expected.previewCodeHealthGate, actual.previewCodeHealthGate)
    }

    private fun assertDefaultState(state: CodeSceneGlobalSettings) {
        assertEquals(defaultServerUrl, state.serverUrl)
        assertTrue(state.enableCodeLenses)
        assertFalse(state.enableAutoRefactor)
        assertTrue(state.excludeGitignoreFiles)
        assertFalse(state.previewCodeHealthGate)
    }

    private fun createCustomState(): CodeSceneGlobalSettings = CodeSceneGlobalSettings(
        foldingRangeProvider = "new-provider",
        defaultEditorFormatter = "new-formatter",
        defaultNotebookFormatter = "new-notebook-formatter",
        serverUrl = "https://new-server.com",
        enableCodeLenses = false,
        enableAutoRefactor = true,
        excludeGitignoreFiles = false,
        previewCodeHealthGate = true
    )
}