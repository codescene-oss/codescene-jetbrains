package com.codescene.jetbrains.core.settings

import com.codescene.jetbrains.core.contracts.ISettingsChangeListener
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.util.Constants.CODESCENE_SERVER_URL
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsStateManagerTest {
    private lateinit var manager: SettingsStateManager
    private lateinit var listener: ISettingsChangeListener

    @Before
    fun setUp() {
        manager = SettingsStateManager()
        listener = mockk(relaxed = true)
        manager.addSettingsChangeListener(listener)
    }

    @Test
    fun `new manager has default CodeSceneGlobalSettings`() {
        val fresh = SettingsStateManager()
        val s = fresh.getState()
        assertEquals(CODESCENE_SERVER_URL, s.serverUrl)
        assertTrue(s.enableCodeLenses)
        assertTrue(s.enableAutoRefactor)
        assertTrue(s.excludeGitignoreFiles)
        assertFalse(s.previewCodeHealthGate)
    }

    @Test
    fun `updateTelemetryConsent updates state and notifies listeners`() {
        manager.updateTelemetryConsent(true)
        assertTrue(manager.getState().telemetryConsentGiven)
        verify(exactly = 1) { listener.onSettingsChanged(any(), any()) }

        manager.updateTelemetryConsent(false)
        assertFalse(manager.getState().telemetryConsentGiven)
        verify(exactly = 2) { listener.onSettingsChanged(any(), any()) }
    }

    @Test
    fun `loadState notifies listeners with old and new state`() {
        val newState = CodeSceneGlobalSettings(serverUrl = "https://custom.example.com")

        manager.loadState(newState)

        verify(exactly = 1) { listener.onSettingsChanged(any(), any()) }
        assertEquals("https://custom.example.com", manager.getState().serverUrl)
    }

    @Test
    fun `removeSettingsChangeListener stops notifications`() {
        manager.removeSettingsChangeListener(listener)

        manager.loadState(CodeSceneGlobalSettings(serverUrl = "https://other.com"))

        verify(exactly = 0) { listener.onSettingsChanged(any(), any()) }
    }

    @Test
    fun `updateAceStatus notifies listeners and updates state`() {
        manager.updateAceStatus(AceStatus.SIGNED_IN)

        assertEquals(AceStatus.SIGNED_IN, manager.getState().aceStatus)
        verify(exactly = 1) { listener.onSettingsChanged(any(), any()) }
    }

    @Test
    fun `updateAceAcknowledged notifies listeners and updates state`() {
        assertFalse(manager.getState().aceAcknowledged)

        manager.updateAceAcknowledged(true)

        assertTrue(manager.getState().aceAcknowledged)
        verify(exactly = 1) { listener.onSettingsChanged(any(), any()) }
    }

    @Test
    fun `notifyIfStateChanged does nothing when state is unchanged`() {
        val snapshot = manager.currentState().copy()

        manager.notifyIfStateChanged(snapshot)

        verify(exactly = 0) { listener.onSettingsChanged(any(), any()) }
    }

    @Test
    fun `notifyIfStateChanged fires when state has changed`() {
        val snapshot = manager.currentState().copy()
        manager.getState().serverUrl = "https://changed.com"

        manager.notifyIfStateChanged(snapshot)

        verify(exactly = 1) { listener.onSettingsChanged(any(), any()) }
    }

    @Test
    fun `multiple listeners all receive notifications`() {
        val second = mockk<ISettingsChangeListener>(relaxed = true)
        manager.addSettingsChangeListener(second)

        manager.updateAceStatus(AceStatus.ERROR)

        verify(exactly = 1) { listener.onSettingsChanged(any(), any()) }
        verify(exactly = 1) { second.onSettingsChanged(any(), any()) }
    }
}
