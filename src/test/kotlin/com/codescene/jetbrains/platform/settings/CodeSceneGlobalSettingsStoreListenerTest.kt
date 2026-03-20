package com.codescene.jetbrains.platform.settings

import com.codescene.jetbrains.core.contracts.ISettingsChangeListener
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CodeSceneGlobalSettingsStoreListenerTest {
    private lateinit var store: CodeSceneGlobalSettingsStore
    private lateinit var listener: ISettingsChangeListener

    @Before
    fun setUp() {
        store = CodeSceneGlobalSettingsStore()
        listener = mockk(relaxed = true)
        store.addSettingsChangeListener(listener)
    }

    @Test
    fun `loadState notifies listeners with old and new state`() {
        val newState = CodeSceneGlobalSettings(serverUrl = "https://custom.example.com")

        store.loadState(newState)

        verify(exactly = 1) { listener.onSettingsChanged(any(), any()) }
        assertEquals("https://custom.example.com", store.state.serverUrl)
    }

    @Test
    fun `removeSettingsChangeListener stops notifications`() {
        store.removeSettingsChangeListener(listener)

        store.loadState(CodeSceneGlobalSettings(serverUrl = "https://other.com"))

        verify(exactly = 0) { listener.onSettingsChanged(any(), any()) }
    }

    @Test
    fun `updateAceStatus notifies listeners and updates state`() {
        store.updateAceStatus(AceStatus.SIGNED_IN)

        assertEquals(AceStatus.SIGNED_IN, store.state.aceStatus)
        verify(exactly = 1) { listener.onSettingsChanged(any(), any()) }
    }

    @Test
    fun `updateAceAcknowledged notifies listeners and updates state`() {
        assertFalse(store.state.aceAcknowledged)

        store.updateAceAcknowledged(true)

        assertTrue(store.state.aceAcknowledged)
        verify(exactly = 1) { listener.onSettingsChanged(any(), any()) }
    }

    @Test
    fun `notifyIfStateChanged does nothing when state is unchanged`() {
        val snapshot = store.currentState().copy()

        store.notifyIfStateChanged(snapshot)

        verify(exactly = 0) { listener.onSettingsChanged(any(), any()) }
    }

    @Test
    fun `notifyIfStateChanged fires when state has changed`() {
        val snapshot = store.currentState().copy()
        store.state.serverUrl = "https://changed.com"

        store.notifyIfStateChanged(snapshot)

        verify(exactly = 1) { listener.onSettingsChanged(any(), any()) }
    }

    @Test
    fun `multiple listeners all receive notifications`() {
        val second = mockk<ISettingsChangeListener>(relaxed = true)
        store.addSettingsChangeListener(second)

        store.updateAceStatus(AceStatus.ERROR)

        verify(exactly = 1) { listener.onSettingsChanged(any(), any()) }
        verify(exactly = 1) { second.onSettingsChanged(any(), any()) }
    }
}
