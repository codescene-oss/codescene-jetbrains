package com.codescene.jetbrains

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettings
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.util.Constants.CODESCENE_SERVER_URL
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CodeSceneGlobalSettingsStoreTest : BasePlatformTestCase() {
    private lateinit var settingsStore: CodeSceneGlobalSettingsStore
    private var defaultServerUrl = CODESCENE_SERVER_URL

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

        assertEquals(newState, settingsStore.state)
    }

    fun `test getState returns the current state`() {
        val currentState = settingsStore.state

        assertNotNull(currentState)
        assertEquals(defaultServerUrl, currentState.serverUrl)
    }

    fun `test updateTermsAndConditionsAcceptance updates the state correctly`() {
        val newState = createCustomState()

        settingsStore.loadState(newState)

        settingsStore.updateTelemetryConsent(true)

        assertTrue(settingsStore.state.telemetryConsentGiven)

        settingsStore.updateTelemetryConsent(false)

        assertFalse(settingsStore.state.telemetryConsentGiven)
    }

    private fun assertDefaultState(state: CodeSceneGlobalSettings) {
        assertEquals(defaultServerUrl, state.serverUrl)
        assertTrue(state.enableCodeLenses)
        assertTrue(state.enableAutoRefactor)
        assertTrue(state.excludeGitignoreFiles)
        assertFalse(state.previewCodeHealthGate)
    }

    private fun createCustomState(): CodeSceneGlobalSettings = CodeSceneGlobalSettings(
        serverUrl = "https://new-server.com",
        enableCodeLenses = true,
        enableAutoRefactor = true,
        excludeGitignoreFiles = false,
        previewCodeHealthGate = true
    )
}